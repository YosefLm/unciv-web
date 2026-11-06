/*
 * Unciv browser peer transport.
 * Copyright 2026 The Unciv contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This module exposes the Unciv API-v1 save endpoints over one reliable,
 * ordered WebRTC data channel. Signaling is intentionally manual: the browser
 * UI exchanges complete SDP offers and answers without a signaling server.
 */
(function (global) {
    "use strict";

    const VIRTUAL_ORIGIN = "https://peer.unciv.invalid";
    const DB_NAME = "unciv-peer-save-v1";
    const DB_VERSION = 1;
    const STORE_NAME = "files";
    const MAX_FILE_BYTES = 32 * 1024 * 1024;
    const MAX_TOTAL_BYTES = 256 * 1024 * 1024;
    const MAX_ACTIVE_REQUESTS = 16;
    const MAX_IN_FLIGHT_BYTES = 64 * 1024 * 1024;
    const CHUNK_BYTES = 16 * 1024;
    const DEFAULT_TIMEOUT_MS = 30000;
    const ICE_GATHER_TIMEOUT_MS = 20000;
    const DEFAULT_ICE_SERVERS = [{ urls: ["stun:stun.l.google.com:19302"] }];

    let peerConnection = null;
    let dataChannel = null;
    let role = null;
    let closed = false;
    let databasePromise = null;
    let nextRequestId = 1;
    let sendTail = Promise.resolve();
    let hostRequestTail = Promise.resolve();
    let localRequestTail = Promise.resolve();
    let peerState = "idle";
    let incomingBytes = 0;
    let pendingResponseBytes = 0;
    const pendingRequests = new Map();
    const incomingRequests = new Map();
    const listeners = new Set();

    function emitState() {
        const state = snapshot();
        for (const listener of Array.from(listeners)) {
            try { listener(state); } catch (error) { console.error("uncivPeer state listener failed", error); }
        }
        global.dispatchEvent(new CustomEvent("uncivpeerstatechange", { detail: state }));
    }

    function snapshot() {
        return {
            state: peerState,
            isHost: role === "host",
            isConnected: !!dataChannel && dataChannel.readyState === "open",
            baseUrl: VIRTUAL_ORIGIN + "/"
        };
    }

    function setState(state) {
        if (peerState !== state) {
            peerState = state;
            emitState();
        }
    }

    function assertWebRtc() {
        if (!global.RTCPeerConnection) throw new Error("This browser does not support WebRTC data channels.");
    }

    function parseIceServers(value) {
        if (value === undefined || value === null) return DEFAULT_ICE_SERVERS;
        if (!Array.isArray(value)) throw new TypeError("iceServers must be an array of RTCIceServer values.");
        return value;
    }

    function closeCurrent(reason) {
        closed = true;
        if (dataChannel) {
            try { dataChannel.close(); } catch (_) { /* already closed */ }
            dataChannel = null;
        }
        if (peerConnection) {
            try { peerConnection.close(); } catch (_) { /* already closed */ }
            peerConnection = null;
        }
        for (const [id, pending] of pendingRequests) {
            pending.cleanup();
            pending.reject(makeNetworkError(reason || "Peer connection closed."));
            pendingRequests.delete(id);
        }
        incomingRequests.clear();
        incomingBytes = 0;
        pendingResponseBytes = 0;
        role = null;
        hostRequestTail = Promise.resolve();
        localRequestTail = Promise.resolve();
        setState("disconnected");
    }

    function makeNetworkError(message) {
        return new TypeError(message || "Peer is not connected.");
    }

    function attachChannel(channel) {
        if (dataChannel && dataChannel !== channel) {
            try { dataChannel.close(); } catch (_) { /* old peer */ }
        }
        dataChannel = channel;
        dataChannel.binaryType = "arraybuffer";
        dataChannel.bufferedAmountLowThreshold = 64 * 1024;
        dataChannel.onopen = function () {
            if (!closed) setState("connected");
        };
        dataChannel.onclose = function () {
            if (!closed) closeCurrent("Peer disconnected.");
        };
        dataChannel.onerror = function () {
            if (!closed) closeCurrent("Peer data channel failed.");
        };
        dataChannel.onmessage = handleMessage;
        if (channel.readyState === "open") setState("connected");
    }

    function attachPeerConnection(pc) {
        peerConnection = pc;
        pc.onconnectionstatechange = function () {
            if (!closed && (pc.connectionState === "failed" || pc.connectionState === "closed")) {
                closeCurrent("WebRTC connection " + pc.connectionState + ".");
            }
        };
        pc.oniceconnectionstatechange = function () {
            if (!closed && pc.iceConnectionState === "failed") closeCurrent("ICE connection failed.");
        };
    }

    function makePeerConnection(iceServers) {
        assertWebRtc();
        closed = false;
        setState("signaling");
        const pc = new RTCPeerConnection({ iceServers: parseIceServers(iceServers) });
        attachPeerConnection(pc);
        return pc;
    }

    function waitForIceGathering(pc, timeoutMs) {
        if (pc.iceGatheringState === "complete") return Promise.resolve();
        return new Promise(function (resolve, reject) {
            let done = false;
            const timer = setTimeout(function () {
                finish();
            }, timeoutMs || ICE_GATHER_TIMEOUT_MS);
            function finish(error) {
                if (done) return;
                done = true;
                clearTimeout(timer);
                pc.removeEventListener("icegatheringstatechange", onChange);
                if (error) reject(error); else resolve();
            }
            function onChange() {
                if (pc.iceGatheringState === "complete") finish();
            }
            pc.addEventListener("icegatheringstatechange", onChange);
            onChange();
        });
    }

    function signalDescription(value, expectedType) {
        let parsed = value;
        if (typeof value === "string") {
            try { parsed = JSON.parse(value); }
            catch (_) { throw new TypeError("Signaling text must be the JSON offer or answer copied from Unciv."); }
        }
        if (!parsed || parsed.type !== expectedType || typeof parsed.sdp !== "string" || !parsed.sdp.trim()) {
            throw new TypeError("Expected a nonempty WebRTC " + expectedType + " description.");
        }
        return { type: expectedType, sdp: parsed.sdp };
    }

    async function createHostOffer(options) {
        options = options || {};
        closeCurrent("Starting a new host session.");
        role = "host";
        const pc = makePeerConnection(options.iceServers);
        pc.ondatachannel = function (event) { attachChannel(event.channel); };
        pc.createDataChannel("unciv-v1", { ordered: true });
        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        await waitForIceGathering(pc, options.iceGatherTimeoutMs);
        if (peerConnection !== pc) throw makeNetworkError("Host session was replaced while creating an offer.");
        return JSON.stringify({ type: pc.localDescription.type, sdp: pc.localDescription.sdp });
    }

    async function acceptAnswer(answer, options) {
        if (role !== "host" || !peerConnection) throw new Error("Create a host offer before accepting an answer.");
        await peerConnection.setRemoteDescription(signalDescription(answer, "answer"));
        setState("connecting");
        return waitForConnected(options && options.connectTimeoutMs);
    }

    async function createJoinAnswer(offer, options) {
        options = options || {};
        closeCurrent("Starting a new join session.");
        role = "client";
        const pc = makePeerConnection(options.iceServers);
        pc.ondatachannel = function (event) { attachChannel(event.channel); };
        await pc.setRemoteDescription(signalDescription(offer, "offer"));
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        await waitForIceGathering(pc, options.iceGatherTimeoutMs);
        if (peerConnection !== pc) throw makeNetworkError("Join session was replaced while creating an answer.");
        setState("connecting");
        return JSON.stringify({ type: pc.localDescription.type, sdp: pc.localDescription.sdp });
    }

    function waitForConnected(timeoutMs) {
        if (dataChannel && dataChannel.readyState === "open") return Promise.resolve();
        return new Promise(function (resolve, reject) {
            const deadline = Date.now() + (timeoutMs || 30000);
            let finished = false;
            const timer = setInterval(function () {
                if (finished) return;
                if (dataChannel && dataChannel.readyState === "open") {
                    finished = true;
                    clearInterval(timer);
                    resolve();
                } else if (peerState === "disconnected" || Date.now() >= deadline) {
                    finished = true;
                    clearInterval(timer);
                    reject(makeNetworkError("WebRTC peer did not connect before the timeout."));
                }
            }, 25);
        });
    }

    function disconnect() {
        closeCurrent("Peer disconnected.");
    }

    function openDatabase() {
        if (databasePromise) return databasePromise;
        databasePromise = new Promise(function (resolve, reject) {
            if (!global.indexedDB) {
                reject(new Error("IndexedDB is unavailable; this browser cannot host persistent peer saves."));
                return;
            }
            const request = indexedDB.open(DB_NAME, DB_VERSION);
            request.onupgradeneeded = function () {
                const db = request.result;
                if (!db.objectStoreNames.contains(STORE_NAME)) db.createObjectStore(STORE_NAME, { keyPath: "name" });
            };
            request.onsuccess = function () { resolve(request.result); };
            request.onerror = function () { reject(request.error || new Error("Could not open peer save storage.")); };
            request.onblocked = function () { reject(new Error("Peer save storage upgrade is blocked by another tab.")); };
        });
        return databasePromise;
    }

    function transactionDone(transaction) {
        return new Promise(function (resolve, reject) {
            transaction.oncomplete = function () { resolve(); };
            transaction.onabort = transaction.onerror = function () { reject(transaction.error || new Error("IndexedDB transaction failed.")); };
        });
    }

    function idbRequest(request) {
        return new Promise(function (resolve, reject) {
            request.onsuccess = function () { resolve(request.result); };
            request.onerror = function () { reject(request.error || new Error("IndexedDB request failed.")); };
        });
    }

    async function localFileRequest(method, fileName, body) {
        const db = await openDatabase();
        if (method === "GET") {
            const tx = db.transaction(STORE_NAME, "readonly");
            const done = transactionDone(tx);
            const row = await idbRequest(tx.objectStore(STORE_NAME).get(fileName));
            await done;
            if (!row) return textResponse(404, "File does not exist");
            return new Response(row.bytes, { status: 200, headers: { "content-type": row.contentType || "text/plain; charset=utf-8" } });
        }
        if (method === "PUT") {
            if (body.byteLength > MAX_FILE_BYTES) return textResponse(413, "Game file is too large!");
            const tx = db.transaction(STORE_NAME, "readwrite");
            const done = transactionDone(tx);
            const store = tx.objectStore(STORE_NAME);
            const allRows = await idbRequest(store.getAll());
            const oldRow = allRows.find(function (row) { return row.name === fileName; });
            const total = allRows.reduce(function (sum, row) { return sum + row.bytes.byteLength; }, 0);
            if (total - (oldRow ? oldRow.bytes.byteLength : 0) + body.byteLength > MAX_TOTAL_BYTES) {
                tx.abort();
                try { await done; } catch (_) { /* expected transaction abort */ }
                return textResponse(507, "Peer save storage quota exceeded.");
            }
            const copy = body.slice().buffer;
            store.put({ name: fileName, bytes: copy, contentType: "text/plain; charset=utf-8", updatedAt: Date.now() });
            await done;
            return new Response(null, { status: 200 });
        }
        if (method === "DELETE") {
            const tx = db.transaction(STORE_NAME, "readwrite");
            const done = transactionDone(tx);
            const store = tx.objectStore(STORE_NAME);
            const existing = await idbRequest(store.get(fileName));
            if (!existing) {
                tx.abort();
                try { await done; } catch (_) { /* expected transaction abort */ }
                return textResponse(404, "File does not exist");
            }
            store.delete(fileName);
            await done;
            return new Response(null, { status: 200 });
        }
        return textResponse(405, "Method not allowed", { allow: "GET, PUT, DELETE" });
    }

    function textResponse(status, text, extraHeaders) {
        const headers = Object.assign({ "content-type": "text/plain; charset=utf-8" }, extraHeaders || {});
        return new Response(status === 204 ? null : text, { status: status, headers: headers });
    }

    function validateEndpoint(url, method) {
        let parsed;
        try { parsed = new URL(url, global.location && global.location.href); }
        catch (_) { throw new TypeError("Invalid peer URL."); }
        if (parsed.origin !== VIRTUAL_ORIGIN) throw new TypeError("Peer transport only serves " + VIRTUAL_ORIGIN + ".");
        if (parsed.username || parsed.password || parsed.search || parsed.hash) throw new TypeError("Peer URLs cannot contain credentials, query, or fragment components.");
        method = String(method || "GET").toUpperCase();
        if (parsed.pathname === "/isalive" && method === "GET") return { method: method, kind: "alive" };
        const match = /^\/files\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(?:_Preview)?)$/.exec(parsed.pathname);
        if (!match) throw new TypeError("Peer requests are limited to /isalive and UUID save files.");
        if (["GET", "PUT", "DELETE"].indexOf(method) < 0) return { method: method, kind: "file", name: match[1], invalidMethod: true };
        return { method: method, kind: "file", name: match[1] };
    }

    async function localRequest(url, init) {
        const method = String(init.method || "GET").toUpperCase();
        const endpoint = validateEndpoint(url, method);
        if (endpoint.invalidMethod) return textResponse(405, "Method not allowed", { allow: "GET, PUT, DELETE" });
        if (endpoint.kind === "alive") {
            return new Response(JSON.stringify({ authVersion: 0, chatVersion: 0 }), {
                status: 200,
                headers: { "content-type": "application/json; charset=utf-8" }
            });
        }
        const body = method === "PUT" ? await requestBodyBytes(init.body) : new Uint8Array(0);
        return localFileRequest(method, endpoint.name, body);
    }

    function serializedLocalRequest(url, init) {
        const operation = localRequestTail.then(function () { return localRequest(url, init); });
        localRequestTail = operation.catch(function () {});
        return operation;
    }

    async function requestBodyBytes(body) {
        if (body === undefined || body === null) return new Uint8Array(0);
        if (typeof body === "string") return new TextEncoder().encode(body);
        if (body instanceof ArrayBuffer) return new Uint8Array(body.slice(0));
        if (ArrayBuffer.isView(body)) return new Uint8Array(body.buffer.slice(body.byteOffset, body.byteOffset + body.byteLength));
        if (global.Blob && body instanceof Blob) return new Uint8Array(await body.arrayBuffer());
        if (global.URLSearchParams && body instanceof URLSearchParams) return new TextEncoder().encode(body.toString());
        throw new TypeError("Peer request body must be a string, Blob, ArrayBuffer, or typed array.");
    }

    function requestHeaders(headers) {
        const result = [];
        if (!headers) return result;
        new Headers(headers).forEach(function (value, name) { result.push([name, value]); });
        return result;
    }

    function fetchInit(input, init) {
        if (input instanceof Request) {
            const method = (init && init.method) || input.method;
            const headers = (init && init.headers) || input.headers;
            const signal = (init && init.signal) || input.signal;
            return { method: method, headers: requestHeaders(headers), signal: signal, body: init && Object.prototype.hasOwnProperty.call(init, "body") ? init.body : input.clone().arrayBuffer() };
        }
        return {
            method: init && init.method || "GET",
            headers: requestHeaders(init && init.headers),
            signal: init && init.signal,
            body: init && init.body,
            peerTimeoutMs: init && init.peerTimeoutMs
        };
    }

    function concatChunks(chunks, length) {
        const output = new Uint8Array(length);
        let offset = 0;
        for (const chunk of chunks) {
            output.set(chunk, offset);
            offset += chunk.byteLength;
        }
        if (offset !== length) throw new Error("Peer transfer length did not match its frame metadata.");
        return output;
    }

    function packChunk(kind, id, data) {
        const frame = new Uint8Array(5 + data.byteLength);
        frame[0] = kind;
        new DataView(frame.buffer).setUint32(1, id, false);
        frame.set(data, 5);
        return frame.buffer;
    }

    function unpackChunk(buffer) {
        const view = new Uint8Array(buffer);
        if (view.byteLength < 5) throw new Error("Malformed peer data frame.");
        return { kind: view[0], id: new DataView(buffer).getUint32(1, false), data: view.subarray(5) };
    }

    function ensureConnected() {
        if (closed || !dataChannel || dataChannel.readyState !== "open") throw makeNetworkError("Peer is not connected.");
    }

    async function sendRaw(message) {
        ensureConnected();
        await waitForBufferCapacity(dataChannel);
        ensureConnected();
        dataChannel.send(message);
    }

    function enqueueSend(operation) {
        const result = sendTail.then(operation);
        sendTail = result.catch(function () {});
        return result;
    }

    async function waitForBufferCapacity(channel) {
        const deadline = Date.now() + DEFAULT_TIMEOUT_MS;
        while (channel.bufferedAmount > 256 * 1024) {
            ensureConnected();
            if (Date.now() >= deadline) throw makeNetworkError("Peer send buffer remained full past the timeout.");
            await new Promise(function (resolve) { setTimeout(resolve, 10); });
        }
    }

    async function sendBody(kind, id, body) {
        for (let offset = 0; offset < body.byteLength; offset += CHUNK_BYTES) {
            const part = body.subarray(offset, Math.min(offset + CHUNK_BYTES, body.byteLength));
            await sendRaw(packChunk(kind, id, part));
        }
    }

    function handleMessage(event) {
        try {
            if (typeof event.data === "string") handleControl(JSON.parse(event.data));
            else if (event.data instanceof ArrayBuffer) handleChunk(unpackChunk(event.data));
            else throw new Error("Unsupported peer data frame type.");
        } catch (error) {
            console.error("Unciv peer protocol error", error);
            closeCurrent("Peer protocol error: " + error.message);
        }
    }

    function handleControl(message) {
        if (!message || typeof message !== "object" || !Number.isInteger(message.id) || message.id < 1) throw new Error("Malformed peer control message.");
        if (message.t === "req") {
            if (role !== "host") throw new Error("Only the host accepts save requests.");
            if (incomingRequests.size >= MAX_ACTIVE_REQUESTS || !Number.isInteger(message.length) || message.length < 0 || message.length > MAX_FILE_BYTES || incomingBytes + message.length > MAX_IN_FLIGHT_BYTES) {
                sendControl({ t: "err", id: message.id, message: "Too many active peer requests." });
                return;
            }
            incomingBytes += message.length;
            incomingRequests.set(message.id, {
                method: message.method,
                url: message.url,
                headers: message.headers,
                expected: message.length,
                received: 0,
                chunks: []
            });
            return;
        }
        if (message.t === "reqEnd") {
            const request = incomingRequests.get(message.id);
            if (!request) throw new Error("Request end without request metadata.");
            incomingRequests.delete(message.id);
            incomingBytes -= request.expected;
            if (request.received !== request.expected) {
                sendControl({ t: "err", id: message.id, message: "Request body length mismatch." });
                return;
            }
            const body = concatChunks(request.chunks, request.expected);
            hostRequestTail = hostRequestTail.then(async function () {
                try {
                    const response = await serializedLocalRequest(request.url, { method: request.method, headers: request.headers, body: body });
                    await sendResponse(message.id, response);
                } catch (error) {
                    await sendControl({ t: "err", id: message.id, message: error && error.message || String(error) });
                }
            });
            return;
        }
        if (message.t === "res") {
            const pending = pendingRequests.get(message.id);
            if (!pending) return;
            if (!Number.isInteger(message.length) || message.length < 0 || message.length > MAX_FILE_BYTES) throw new Error("Invalid or oversized response body.");
            if (pendingResponseBytes + message.length > MAX_IN_FLIGHT_BYTES) throw new Error("Too many active peer response bytes.");
            pendingResponseBytes += message.length;
            pending.status = message.status;
            pending.statusText = message.statusText || "";
            pending.headers = message.headers || [];
            pending.expected = message.length;
            pending.received = 0;
            pending.chunks = [];
            return;
        }
        if (message.t === "resEnd") {
            const pending = pendingRequests.get(message.id);
            if (!pending || pending.expected === undefined) return;
            if (pending.received !== pending.expected) {
                settlePending(message.id, false, new Error("Response body length mismatch."));
                return;
            }
            const bytes = concatChunks(pending.chunks, pending.expected);
            const response = new Response(pending.status === 204 ? null : bytes, {
                status: pending.status,
                statusText: pending.statusText,
                headers: pending.headers
            });
            settlePending(message.id, true, response);
            return;
        }
        if (message.t === "err") {
            settlePending(message.id, false, makeNetworkError(message.message || "Peer request failed."));
            return;
        }
        throw new Error("Unknown peer control message.");
    }

    function handleChunk(frame) {
        const record = frame.kind === 0 ? incomingRequests.get(frame.id) : frame.kind === 1 ? pendingRequests.get(frame.id) : null;
        if (!record) return;
        if (record.received + frame.data.byteLength > record.expected) throw new Error("Peer data exceeded its announced transfer length.");
        record.chunks.push(frame.data.slice());
        record.received += frame.data.byteLength;
    }

    function sendControl(message) {
        return enqueueSend(function () { return sendRaw(JSON.stringify(message)); });
    }

    async function sendResponse(id, response) {
        const body = new Uint8Array(await response.arrayBuffer());
        if (body.byteLength > MAX_FILE_BYTES) {
            await sendControl({ t: "err", id: id, message: "Host response exceeds the peer transfer limit." });
            return;
        }
        await enqueueSend(async function () {
            await sendRaw(JSON.stringify({
                t: "res", id: id, status: response.status, statusText: response.statusText,
                headers: Array.from(response.headers.entries()), length: body.byteLength
            }));
            await sendBody(1, id, body);
            await sendRaw(JSON.stringify({ t: "resEnd", id: id }));
        });
    }

    function settlePending(id, success, value) {
        const pending = pendingRequests.get(id);
        if (!pending) return;
        pendingRequests.delete(id);
        if (Number.isInteger(pending.expected)) pendingResponseBytes -= pending.expected;
        pending.cleanup();
        if (success) pending.resolve(value); else pending.reject(value);
    }

    function request(url, init) {
        init = init || {};
        const request = fetchInit(url, init);
        const method = String(request.method || "GET").toUpperCase();
        const resolvedUrl = url instanceof Request ? url.url : String(url);
        let endpoint;
        try { endpoint = validateEndpoint(resolvedUrl, method); }
        catch (error) { return Promise.reject(error); }
        if (endpoint.invalidMethod) return Promise.resolve(textResponse(405, "Method not allowed", { allow: "GET, PUT, DELETE" }));
        if (role === "host") return serializedLocalRequest(resolvedUrl, request);
        if (role !== "client" || !dataChannel || dataChannel.readyState !== "open") return Promise.reject(makeNetworkError("Join a connected peer host before making save requests."));
        if (pendingRequests.size >= MAX_ACTIVE_REQUESTS) return Promise.reject(makeNetworkError("Too many active peer requests."));

        return Promise.resolve(request.body).then(requestBodyBytes).then(function (body) {
            if (body.byteLength > MAX_FILE_BYTES) throw new TypeError("Peer request body exceeds the 32 MiB limit.");
            return new Promise(function (resolve, reject) {
                const id = nextRequestId++;
                const timeoutMs = Number(request.peerTimeoutMs || DEFAULT_TIMEOUT_MS);
                const timer = setTimeout(function () { settlePending(id, false, makeNetworkError("Peer request timed out.")); }, timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS);
                const signal = request.signal;
                const abort = function () { settlePending(id, false, signal.reason || new DOMException("The operation was aborted.", "AbortError")); };
                const cleanup = function () {
                    clearTimeout(timer);
                    if (signal) signal.removeEventListener("abort", abort);
                };
                pendingRequests.set(id, { resolve: resolve, reject: reject, cleanup: cleanup });
                if (signal) {
                    if (signal.aborted) { abort(); return; }
                    signal.addEventListener("abort", abort, { once: true });
                }
                enqueueSend(async function () {
                    await sendRaw(JSON.stringify({ t: "req", id: id, method: method, url: resolvedUrl, headers: request.headers, length: body.byteLength }));
                    await sendBody(0, id, body);
                    await sendRaw(JSON.stringify({ t: "reqEnd", id: id }));
                }).catch(function (error) { settlePending(id, false, error); });
            });
        });
    }

    function onStateChange(listener) {
        if (typeof listener !== "function") throw new TypeError("State listener must be a function.");
        listeners.add(listener);
        listener(snapshot());
        return function () { listeners.delete(listener); };
    }

    function mountSetupUi(container, options) {
        options = options || {};
        container = container || document.body;
        const root = document.createElement("section");
        root.className = "unciv-peer-setup";
        root.innerHTML = [
            "<style>",
            ".unciv-peer-setup{font:14px system-ui,sans-serif;max-width:680px;padding:16px;border:1px solid #888;border-radius:8px;background:#fff;color:#222}",
            ".unciv-peer-setup textarea{box-sizing:border-box;width:100%;min-height:90px;font:12px monospace;margin:6px 0}",
            ".unciv-peer-setup button{margin:4px;padding:6px 10px}.unciv-peer-setup label{display:block;margin-top:8px}",
            "</style>",
            "<h2>Unciv peer multiplayer</h2>",
            "<p>Host saves stay on this browser. Exchange the offer and answer with your peer. Internet connections may require STUN or TURN support.</p>",
            "<label>ICE server JSON (blank uses public Google STUN)<textarea data-ice placeholder='[{\"urls\":\"turn:example.org\",\"username\":\"user\",\"credential\":\"password\"}]'></textarea></label>",
            "<div><button type=button data-host>Create host offer</button><button type=button data-join>Create join answer</button></div>",
            "<label>Offer from host / answer from peer<textarea data-input spellcheck=false></textarea></label>",
            "<div><button type=button data-accept>Accept host answer</button><button type=button data-disconnect>Disconnect</button></div>",
            "<label>Answer to share<textarea data-output readonly spellcheck=false></textarea></label>",
            "<p data-status>Disconnected</p>"
        ].join("");
        const input = root.querySelector("[data-input]");
        const output = root.querySelector("[data-output]");
        const status = root.querySelector("[data-status]");
        const ice = root.querySelector("[data-ice]");
        function iceConfig() {
            if (!ice.value.trim()) return undefined;
            const value = JSON.parse(ice.value);
            if (!Array.isArray(value)) throw new TypeError("ICE server JSON must be an array.");
            return value;
        }
        root.querySelector("[data-host]").addEventListener("click", async function () {
            try { role = "host"; output.value = await createHostOffer({ iceServers: iceConfig() }); status.textContent = "Offer ready. Send it to the joining player."; }
            catch (error) { status.textContent = error.message; }
        });
        root.querySelector("[data-join]").addEventListener("click", async function () {
            try { role = "client"; output.value = await createJoinAnswer(input.value, { iceServers: iceConfig() }); status.textContent = "Answer ready. Send it to the host."; }
            catch (error) { status.textContent = error.message; }
        });
        root.querySelector("[data-accept]").addEventListener("click", async function () {
            try { await acceptAnswer(input.value); status.textContent = "Connected. Server URL: " + VIRTUAL_ORIGIN + "/"; if (options.onConnected) options.onConnected(VIRTUAL_ORIGIN + "/"); }
            catch (error) { status.textContent = error.message; }
        });
        root.querySelector("[data-disconnect]").addEventListener("click", function () { disconnect(); status.textContent = "Disconnected"; });
        const removeListener = onStateChange(function (state) {
            status.textContent = state.isConnected ? (state.isHost ? "Hosting" : "Connected to host") + ". Server URL: " + state.baseUrl : state.state;
            if (state.isConnected && options.onConnected) options.onConnected(state.baseUrl);
        });
        container.appendChild(root);
        return { element: root, dispose: function () { removeListener(); root.remove(); } };
    }

    const api = {
        virtualOrigin: VIRTUAL_ORIGIN,
        get baseUrl() { return VIRTUAL_ORIGIN + "/"; },
        get isConnected() { return !!dataChannel && dataChannel.readyState === "open"; },
        get isHost() { return role === "host"; },
        get state() { return peerState; },
        createHostOffer: createHostOffer,
        acceptAnswer: acceptAnswer,
        createJoinAnswer: createJoinAnswer,
        request: request,
        disconnect: disconnect,
        onStateChange: onStateChange,
        mountSetupUi: mountSetupUi,
        limits: Object.freeze({ maxFileBytes: MAX_FILE_BYTES, maxTotalBytes: MAX_TOTAL_BYTES, timeoutMs: DEFAULT_TIMEOUT_MS, chunkBytes: CHUNK_BYTES })
    };

    global.uncivPeer = api;
})(window);
