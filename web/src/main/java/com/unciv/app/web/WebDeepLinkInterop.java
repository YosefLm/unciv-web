package com.unciv.app.web;

import org.teavm.jso.JSBody;

public final class WebDeepLinkInterop {
    private WebDeepLinkInterop() {
    }

    @JSBody(params = "value", script =
            "try { return decodeURIComponent(value || ''); } catch (err) { return value || ''; }")
    public static native String decodeURIComponent(String value);
}
