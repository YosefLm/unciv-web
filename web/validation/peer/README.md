# WebRTC peer transport draft

**Deferred work: not integrated into Unciv and not yet validated.** This branch
preserves an initial transport prototype and proposed tests for a later PR.
The main browser revival does not include or enable this module.

This browser-only contract starts two isolated Chromium browser contexts on localhost, exchanges complete SDP offer/answer JSON through the test runner, and exercises the production peer module over an actual WebRTC data channel. The test covers `/isalive`, chunked UTF-8 save and preview PUT/GET, missing/deleted files, reload and reconnection with host IndexedDB persistence, invalid URL and oversize rejection, and request failure on disconnect.

The public entrypoint is `window.uncivPeer.request(url, init): Promise<Response>`. It only handles the virtual origin `https://peer.unciv.invalid`; it is not a network host. A connected host serves API-v1 save requests and persists files in IndexedDB. The client uses the negotiated reliable ordered data channel. The transport is unauthenticated (`authVersion: 0`), and this mode does not implement API-v1 chat (`chatVersion: 0`). A host currently supports one connected peer at a time.

The setup panel is created with `uncivPeer.mountSetupUi(container, { onConnected })`. One player creates an offer; the other pastes it and returns an answer; the host pastes that answer. The entire SDP (including gathered ICE candidates) is exchanged manually. By default the module configures Google public STUN (`stun:stun.l.google.com:19302`). The setup panel accepts a JSON array of RTCIceServer values for a TURN service. Public STUN does not guarantee connectivity across every NAT or firewall; a TURN service may be needed, and the users must exchange signaling out of band.

Run the browser contract with Node.js and Playwright installed:

```powershell
# Optional: set PLAYWRIGHT_MODULE and CHROME_PATH for an explicit local installation.
node web/validation/peer/run.cjs
```

The peer RPC limit mirrors the built-in Unciv server's 32 MiB upload bound, limits IndexedDB use to 256 MiB, uses 16 KiB binary chunks with data-channel backpressure, and times out requests after 30 seconds by default. HTTP file bodies are stored byte-for-byte on the host. A page unload cannot guarantee a final asynchronous cleanup; close or disconnect explicitly when possible.
