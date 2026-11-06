package com.unciv.app.web

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration
import com.unciv.UncivGame
import com.unciv.ui.components.fonts.Fonts
import com.unciv.utils.Display
import com.unciv.utils.PlatformDisplay

object WebLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        Display.platform = object : PlatformDisplay {}
        Fonts.fontImplementation = WebFont()

        val config = WebApplicationConfiguration("canvas").apply {
            width = 0
            height = 0
            useGL30 = true
            storagePrefix = "unciv"
            localStoragePrefix = "unciv"
        }
        WebApplication(WebUncivGame(), config)
    }
}

private class WebUncivGame : UncivGame() {
    override val supportsOnlineMultiplayer = false

    override fun getGcCount(): Int? = null
}
