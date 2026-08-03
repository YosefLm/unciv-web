package com.unciv.platform

import com.badlogic.gdx.scenes.scene2d.ui.TextField

/** JVM text-input bridge kept behind a web no-op overlay. */
object PlatformTextInput {
    @JvmStatic
    fun setKeyboardVisible(keyboard: TextField.OnscreenKeyboard, visible: Boolean) {
        runCatching {
            keyboard.javaClass.getMethod("show", Boolean::class.javaPrimitiveType).invoke(keyboard, visible)
        }
    }
}
