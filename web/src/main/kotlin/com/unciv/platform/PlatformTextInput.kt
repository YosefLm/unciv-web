package com.unciv.platform

import com.badlogic.gdx.scenes.scene2d.ui.TextField

/** TeaVM does not expose the desktop GDX onscreen-keyboard method. */
object PlatformTextInput {
    @JvmStatic
    fun setKeyboardVisible(keyboard: TextField.OnscreenKeyboard, visible: Boolean) {
        // Browser input is handled by the backend's DOM bridge.
    }
}
