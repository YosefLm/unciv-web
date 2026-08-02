package com.unciv.app.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebPreloadApplicationListener;

/**
 * Uses an Unciv asset for TeaVM's optional logo preloader and keeps the full asset preload.
 */
final class WebAssetPreloadApplicationListener extends WebPreloadApplicationListener {
    WebAssetPreloadApplicationListener() {
        startupLogo = "ExtraImages/banner.png";
    }
}
