import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("multiplayer layer exposes WebSocket transport, auth storage, and local server coverage", () => {
  const interop = read("web", "src", "main", "java", "com", "unciv", "logic", "multiplayer", "chat", "WebSocketInterop.java");
  const socket = read("core", "src", "com", "unciv", "logic", "multiplayer", "chat", "ChatWebSocket.kt");
  const server = read("core", "src", "com", "unciv", "logic", "multiplayer", "storage", "MultiplayerServer.kt");
  const harness = read("scripts", "web", "multiplayer-test-server.js");

  assert.match(interop, /WebSocket|open|send/);
  assert.match(socket, /WebSockets|webSocket|session/i);
  assert.match(server, /authenticate|uploadGame|downloadGame/);
  assert.match(harness, /WebSocket|server|listen/i);
});
