package com.unciv.logic.multiplayer;

import org.teavm.jso.JSBody;

final class WebMultiplayerDelay {
    private WebMultiplayerDelay() {
    }

    @JSBody(
            params = {"runnable", "delayMs"},
            script = "setTimeout(function(){ runnable.$run(); }, delayMs);")
    static native void schedule(Runnable runnable, int delayMs);
}
