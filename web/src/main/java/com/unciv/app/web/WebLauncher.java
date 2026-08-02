package com.unciv.app.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.unciv.logic.files.FileChooser;
import com.unciv.logic.files.PlatformSaverLoader;
import com.unciv.logic.files.UncivFiles;
import com.unciv.logic.files.WebFileChooser;
import com.unciv.logic.files.WebPlatformSaverLoader;
import com.unciv.platform.PlatformCapabilities;
import com.unciv.ui.components.fonts.Fonts;
import com.unciv.utils.Display;
import com.unciv.utils.Log;

public class WebLauncher {
    public static void main(String[] args) {
        PlatformCapabilities.setCurrent(PlatformCapabilities.webPhase1());
        Display.INSTANCE.setPlatform(new WebDisplay());
        Fonts.INSTANCE.setFontImplementation(new WebFont());
        boolean customFileChooser = PlatformCapabilities.current.getCustomFileChooser();
        if (customFileChooser) {
            UncivFiles.Companion.setSaverLoader(new WebPlatformSaverLoader());
            FileChooser.platformLoadDialog = (filter, listener) -> WebFileChooser.INSTANCE.openLoadDialog(filter, listener);
        } else {
            UncivFiles.Companion.setSaverLoader(PlatformSaverLoader.Companion.getNone());
            FileChooser.platformLoadDialog = null;
        }
        UncivFiles.Companion.setPreferExternalStorage(false);
        Log.INSTANCE.setBackend(new WebLogBackend());

        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width = 0;
        config.height = 0;
        config.useGL30 = true;
        config.showDownloadLogs = true;

        new WebApplication(new WebGame(), config);
    }
}
