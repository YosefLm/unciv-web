"use strict";
const fs = require("node:fs");
const http = require("node:http");
const path = require("node:path");
const { pathToFileURL } = require("node:url");

function loadPlaywright() {
    if (process.env.PLAYWRIGHT_MODULE) return require(process.env.PLAYWRIGHT_MODULE);
    return require("playwright");
}

const root = path.resolve(__dirname, "../..");
const peerScript = path.join(root, "src/main/resources/peer/unciv-peer.js");
const testHtml = path.join(__dirname, "index.html");
const mimeTypes = { ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8" };
const server = http.createServer((request, response) => {
    const route = request.url === "/" || request.url === "/index.html" ? testHtml : request.url === "/peer/unciv-peer.js" ? peerScript : null;
    if (!route) { response.writeHead(404).end(); return; }
    response.writeHead(200, { "content-type": mimeTypes[path.extname(route)] || "application/octet-stream", "cache-control": "no-store" });
    fs.createReadStream(route).pipe(response);
});

function assert(value, message) { if (!value) throw new Error(message); }
async function waitConnected(page, timeoutMs = 20000) {
    await page.waitForFunction(() => window.uncivPeer.isConnected, null, { timeout: timeoutMs });
}
async function expectReject(page, code) {
    const result = await page.evaluate(async (requestCode) => {
        try {
            if (requestCode === "bad-url") await uncivPeer.request("https://example.invalid/files/00000000-0000-0000-0000-000000000000");
            else if (requestCode === "oversize") await uncivPeer.request(uncivPeer.virtualOrigin + "/files/00000000-0000-0000-0000-000000000000", { method: "PUT", body: new Uint8Array(uncivPeer.limits.maxFileBytes + 1) });
            else await uncivPeer.request(uncivPeer.virtualOrigin + "/isalive");
            return "resolved";
        } catch (error) { return error.name + ":" + error.message; }
    }, code);
    assert(result !== "resolved", "Expected peer request failure for " + code);
    return result;
}
async function connectPair(hostPage, joinPage) {
    const offer = await hostPage.evaluate(() => uncivPeer.createHostOffer({ iceServers: [], iceGatherTimeoutMs: 5000 }));
    const answer = await joinPage.evaluate((sdp) => uncivPeer.createJoinAnswer(sdp, { iceServers: [], iceGatherTimeoutMs: 5000 }), offer);
    const hostConnect = hostPage.evaluate((sdp) => uncivPeer.acceptAnswer(sdp, { connectTimeoutMs: 20000 }), answer);
    await Promise.all([hostConnect, waitConnected(joinPage)]);
    await waitConnected(hostPage);
}

(async () => {
    let browser;
    try {
        await new Promise((resolve, reject) => server.listen(0, "127.0.0.1", error => error ? reject(error) : resolve()));
        const port = server.address().port;
        const { chromium } = loadPlaywright();
        const executablePath = process.env.CHROME_PATH;
        browser = await chromium.launch({ headless: true, executablePath, args: ["--no-sandbox", "--disable-dev-shm-usage"] });
        const hostContext = await browser.newContext();
        const joinContext = await browser.newContext();
        let hostPage = await hostContext.newPage();
        let joinPage = await joinContext.newPage();
        const pageErrors = [];
        for (const page of [hostPage, joinPage]) page.on("pageerror", error => pageErrors.push(error.message));
        await Promise.all([hostPage.goto(`http://127.0.0.1:${port}/`), joinPage.goto(`http://127.0.0.1:${port}/`)]);
        await Promise.all([hostPage.waitForFunction(() => !!window.uncivPeer), joinPage.waitForFunction(() => !!window.uncivPeer)]);

        await connectPair(hostPage, joinPage);
        const status = await joinPage.evaluate(async () => {
            const response = await uncivPeer.request(uncivPeer.virtualOrigin + "/isalive");
            return { status: response.status, body: await response.json() };
        });
        assert(status.status === 200 && status.body.authVersion === 0 && status.body.chatVersion === 0, "Unexpected /isalive response: " + JSON.stringify(status));

        const gameId = "91b9c53a-259d-4b3c-8979-c73c31fe6a17";
        const previewId = gameId + "_Preview";
        const gameText = "unciv-peer-fixture:" + "λ🙂".repeat(180000);
        const upload = await joinPage.evaluate(async ({ gameId, gameText }) => {
            const response = await uncivPeer.request(uncivPeer.virtualOrigin + "/files/" + gameId, { method: "PUT", headers: { "content-type": "text/plain" }, body: gameText });
            return response.status;
        }, { gameId, gameText });
        assert(upload === 200, "Game save PUT returned " + upload);
        const previewUpload = await joinPage.evaluate(async ({ previewId, text }) => {
            return (await uncivPeer.request(uncivPeer.virtualOrigin + "/files/" + previewId, { method: "PUT", body: text })).status;
        }, { previewId, text: "preview" });
        assert(previewUpload === 200, "Preview PUT failed");
        const fetched = await joinPage.evaluate(async (file) => {
            const response = await uncivPeer.request(uncivPeer.virtualOrigin + "/files/" + file);
            return { status: response.status, text: await response.text(), type: response.headers.get("content-type") };
        }, gameId);
        assert(fetched.status === 200 && fetched.text === gameText, "Chunked cross-peer save GET did not match the uploaded UTF-8 body");
        assert(fetched.type && fetched.type.indexOf("text/plain") === 0, "Save response content type missing");
        const missing = await joinPage.evaluate(async () => (await uncivPeer.request(uncivPeer.virtualOrigin + "/files/00000000-0000-0000-0000-000000000000")).status);
        assert(missing === 404, "Missing save did not return 404");
        const rejectedUrl = await expectReject(joinPage, "bad-url");
        const rejectedSize = await expectReject(joinPage, "oversize");
        const deleteStatus = await joinPage.evaluate(async file => (await uncivPeer.request(uncivPeer.virtualOrigin + "/files/" + file, { method: "DELETE" })).status, previewId);
        assert(deleteStatus === 200, "Preview DELETE failed");
        const deleted = await joinPage.evaluate(async file => (await uncivPeer.request(uncivPeer.virtualOrigin + "/files/" + file)).status, previewId);
        assert(deleted === 404, "Deleted preview remained available");

        await Promise.all([hostPage.reload(), joinPage.reload()]);
        await Promise.all([hostPage.waitForFunction(() => !!window.uncivPeer), joinPage.waitForFunction(() => !!window.uncivPeer)]);
        await connectPair(hostPage, joinPage);
        const afterReload = await joinPage.evaluate(async file => {
            const response = await uncivPeer.request(uncivPeer.virtualOrigin + "/files/" + file);
            return { status: response.status, text: await response.text() };
        }, gameId);
        assert(afterReload.status === 200 && afterReload.text === gameText, "Host IndexedDB save did not survive reload and peer reconnection");

        const pendingFailure = await joinPage.evaluate(async () => {
            const pending = uncivPeer.request(uncivPeer.virtualOrigin + "/isalive").then(() => "resolved", error => error.name);
            uncivPeer.disconnect();
            return pending;
        });
        assert(pendingFailure !== "resolved", "In-flight request was not rejected after disconnect");
        const disconnectedFailure = await expectReject(joinPage, "disconnected");
        assert(pageErrors.length === 0, "Browser page errors: " + pageErrors.join(" | "));
        console.log(JSON.stringify({
            result: "PASS",
            contexts: 2,
            status,
            chunkedBodyBytes: Buffer.byteLength(gameText),
            reloadReconnect: afterReload.status,
            pendingDisconnectError: pendingFailure,
            invalidUrlError: rejectedUrl,
            oversizedError: rejectedSize,
            disconnectedError: disconnectedFailure
        }, null, 2));
        await hostContext.close();
        await joinContext.close();
    } catch (error) {
        console.error("peer browser contract failed:", error && error.stack || error);
        process.exitCode = 1;
    } finally {
        if (browser) await browser.close();
        await new Promise(resolve => server.close(resolve));
    }
})();
