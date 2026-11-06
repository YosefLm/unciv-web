let passed = false;
let abortObserved = false;
let bodyCancelObserved = false;
let requestTimeoutObserved = false;
let statusRequest = null;
let postedBody = null;
let runtimeFailure = null;
const cancellationRepro = process.env.FETCH_COROUTINE_CANCEL_REPRO === "1";
const skipCancellation = process.env.FETCH_SKIP_CANCEL === "1";
const successMarker = cancellationRepro
    ? "Coroutine cancellation reproduction passed"
    : "Fetch engine contract passed";
const timeout = setTimeout(() => {
    console.error("Fetch engine contract timed out");
    process.exit(2);
}, 10000);

global.fetch = async (url, options) => {
    if (url.endsWith("/status")) {
        statusRequest = options;
        const body = new ReadableStream({
            start(controller) {
                const encoder = new TextEncoder();
                const bytes = encoder.encode("streamed café 🧭 response body");
                const split = bytes.indexOf(0xC3) + 1; // Split the two-byte UTF-8 encoding of "é".
                controller.enqueue(bytes.slice(0, split));
                controller.enqueue(bytes.slice(split));
                controller.close();
            }
        });
        return new Response(body, {
            status: 207,
            statusText: "Multi-Status",
            headers: { "X-Fetch-Probe": "streamed" }
        });
    }
    if (url.endsWith("/post")) {
        postedBody = new TextDecoder().decode(options.body);
        return new Response("post accepté 🏁", { status: 201 });
    }
    if (url.endsWith("/error")) {
        return new Response("server unavailable", { status: 503 });
    }
    if (url.endsWith("/mid-body")) {
        const body = new ReadableStream({
            start(controller) {
                controller.enqueue(new TextEncoder().encode("partial body"));
            },
            cancel() {
                bodyCancelObserved = true;
            }
        });
        return new Response(body, { status: 200 });
    }
    if (url.endsWith("/network")) {
        return Promise.reject(new TypeError("network offline"));
    }
    if (url.endsWith("/abort")) {
        return new Promise((resolve, reject) => {
            options.signal.addEventListener("abort", () => {
                abortObserved = true;
                reject(new DOMException("Aborted", "AbortError"));
            }, { once: true });
        });
    }
    if (url.endsWith("/request-timeout")) {
        return new Promise((resolve, reject) => {
            options.signal.addEventListener("abort", () => {
                requestTimeoutObserved = true;
                reject(new DOMException("Aborted", "AbortError"));
            }, { once: true });
        });
    }
    throw new Error(`Unexpected Fetch URL: ${url}`);
};

const originalInfo = console.info;
console.info = (...args) => {
    const line = args.join(" ");
    originalInfo(...args);
    if (line === successMarker) passed = true;
};
const originalLog = console.log;
console.log = (...args) => {
    const line = args.join(" ");
    originalLog(...args);
    if (line === successMarker) passed = true;
};
const originalError = console.error;
console.error = (...args) => {
    if (String(args[0]).startsWith("SLF4J(W):")) {
        originalError(...args);
        return;
    }
    runtimeFailure = args.map(String).join(" ");
    originalError(...args);
};

process.on("unhandledRejection", error => {
    runtimeFailure = error?.stack ?? String(error);
});
process.on("uncaughtException", error => {
    runtimeFailure = error?.stack ?? String(error);
});

const runtime = require("./build/generated/teavm/js/unciv-fetch-engine-validation.js");
if (typeof runtime.main !== "function") throw new Error("TeaVM Fetch entry point was not generated");
runtime.main(cancellationRepro ? ["--cancel-repro"] : (skipCancellation ? ["--skip-cancel"] : []), error => {
    if (error) {
        clearTimeout(timeout);
        console.error("Fetch engine contract failed", error?.stack ?? error);
        process.exit(1);
    }
});

const checkResult = setInterval(() => {
    if (runtimeFailure) {
        clearInterval(checkResult);
        clearTimeout(timeout);
        console.error("Fetch engine contract failed", runtimeFailure);
        process.exit(1);
    }
    if (!passed) return;
    clearInterval(checkResult);
    clearTimeout(timeout);
    const header = statusRequest?.headers?.get("X-Unciv-Probe");
    if (!cancellationRepro && (header !== "preserved" || postedBody !== "request café 🧭" ||
        (!skipCancellation && (!abortObserved || !bodyCancelObserved || !requestTimeoutObserved)))) {
        console.error("Fetch engine contract failed", { passed, abortObserved, bodyCancelObserved, requestTimeoutObserved, header, postedBody });
        process.exit(1);
    }
    process.exit(0);
}, 25);
