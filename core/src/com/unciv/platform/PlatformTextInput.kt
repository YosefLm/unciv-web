package com.unciv.platform

import com.badlogic.gdx.scenes.scene2d.ui.TextField

/** Keeps GDX on-screen keyboard calls behind a web-compatible boundary. */
object PlatformTextInput {
    @JvmStatic
    fun setKeyboardVisible(keyboard: TextField.OnscreenKeyboard, visible: Boolean) {
        // Keep the JVM behavior without putting the optional GDX method in the
        // bytecode call graph used by TeaVM's web class library.
        runCatching {
            keyboard.javaClass.getMethod("show", Boolean::class.javaPrimitiveType)
                .invoke(keyboard, visible)
        }
    }
}
