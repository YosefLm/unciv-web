package com.unciv.app.web;

import org.teavm.jso.JSBody;

/**
 * Keeps the web backend's visibility callback from surfacing its known
 * lifecycle race when a preload/test listener has already been replaced.
 * JVM and core code do not use this boundary.
 */
final class WebVisibilityCompat {
    private WebVisibilityCompat() {
    }

    static void install() {
        installGuard();
    }

    @JSBody(script =
            "if (typeof document === 'undefined' || document.__uncivVisibilityCompatInstalled) return;"
                    + "document.__uncivVisibilityCompatInstalled = true;"
                    + "var add = document.addEventListener;"
                    + "var remove = document.removeEventListener;"
                    + "var wrapped = [];"
                    + "document.addEventListener = function(type, listener, options) {"
                    + "  if (type !== 'visibilitychange' || listener == null) return add.call(this, type, listener, options);"
                    + "  var safe = function(event) {"
                    + "    try {"
                    + "      if (typeof listener === 'function') return listener.call(this, event);"
                    + "      if (typeof listener.handleEvent === 'function') return listener.handleEvent(event);"
                    + "    } catch (error) {"
                    + "      var message = String(error && (error.stack || error.message) || error);"
                    + "      if (message.indexOf(\"Cannot read properties of null (reading '$pause')\") >= 0) return;"
                    + "      throw error;"
                    + "    }"
                    + "  };"
                    + "  wrapped.push({ listener: listener, safe: safe });"
                    + "  return add.call(this, type, safe, options);"
                    + "};"
                    + "document.removeEventListener = function(type, listener, options) {"
                    + "  if (type === 'visibilitychange') {"
                    + "    for (var i = wrapped.length - 1; i >= 0; i--) {"
                    + "      if (wrapped[i].listener === listener) {"
                    + "        var safe = wrapped[i].safe;"
                    + "        wrapped.splice(i, 1);"
                    + "        return remove.call(this, type, safe, options);"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "  return remove.call(this, type, listener, options);"
                    + "};")
    private static native void installGuard();
}
