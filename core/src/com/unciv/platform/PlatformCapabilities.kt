package com.unciv.platform

object PlatformCapabilities {
    fun interface LocaleComparatorBridge {
        fun compare(languageTag: String, first: String?, second: String?): Int
    }

    enum class WebProfile { PHASE1, PHASE3_ALPHA, PHASE3_BETA, PHASE3_FULL, PHASE4_FULL }
    enum class CapabilityStage { DISABLED, ALPHA, BETA, ENABLED }

    data class Features(
        val onlineMultiplayer: Boolean = true,
        val customFileChooser: Boolean = true,
        val onlineModDownloads: Boolean = true,
        val systemFontEnumeration: Boolean = true,
        val backgroundThreadPools: Boolean = true,
    )

    data class Staging(
        val onlineMultiplayer: CapabilityStage = CapabilityStage.ENABLED,
        val customFileChooser: CapabilityStage = CapabilityStage.ENABLED,
        val onlineModDownloads: CapabilityStage = CapabilityStage.ENABLED,
        val systemFontEnumeration: CapabilityStage = CapabilityStage.ENABLED,
    )

    @JvmField var current: Features = Features()
    @JvmField var currentStaging: Staging = Staging()
    @JvmField var localeComparatorBridge: LocaleComparatorBridge? = null

    @JvmStatic fun setCurrent(features: Features) { current = features }
    @JvmStatic fun setCurrentStaging(staging: Staging) { currentStaging = staging }
    @JvmStatic fun setLocaleComparatorBridge(bridge: LocaleComparatorBridge?) { localeComparatorBridge = bridge }

    @JvmStatic fun webDefaultsProfile(): WebProfile = WebProfile.PHASE4_FULL

    @JvmStatic fun webPhase1() = Features(
        onlineMultiplayer = false,
        customFileChooser = false,
        onlineModDownloads = false,
        systemFontEnumeration = false,
        backgroundThreadPools = false,
    )

    @JvmStatic fun webPhase3Alpha() = Features(customFileChooser = true, onlineModDownloads = false, onlineMultiplayer = false, systemFontEnumeration = false, backgroundThreadPools = false)
    @JvmStatic fun webPhase3Beta() = Features(customFileChooser = true, onlineModDownloads = true, onlineMultiplayer = false, systemFontEnumeration = false, backgroundThreadPools = false)
    @JvmStatic fun webPhase3Full() = Features(onlineMultiplayer = true, customFileChooser = true, onlineModDownloads = true, systemFontEnumeration = false, backgroundThreadPools = false)
    @JvmStatic fun webPhase4Full() = webPhase3Full()

    @JvmStatic fun webPhase3Staging() = Staging(
        onlineMultiplayer = CapabilityStage.ALPHA,
        customFileChooser = CapabilityStage.BETA,
        onlineModDownloads = CapabilityStage.ALPHA,
        systemFontEnumeration = CapabilityStage.DISABLED,
    )

    @JvmStatic fun webPhase4Staging() = Staging(
        onlineMultiplayer = CapabilityStage.ENABLED,
        customFileChooser = CapabilityStage.ENABLED,
        onlineModDownloads = CapabilityStage.ENABLED,
        systemFontEnumeration = CapabilityStage.DISABLED,
    )

    @JvmStatic fun webProfileFeatures(profile: WebProfile) = when (profile) {
        WebProfile.PHASE1 -> webPhase1()
        WebProfile.PHASE3_ALPHA -> webPhase3Alpha()
        WebProfile.PHASE3_BETA -> webPhase3Beta()
        WebProfile.PHASE3_FULL -> webPhase3Full()
        WebProfile.PHASE4_FULL -> webPhase4Full()
    }

    @JvmStatic fun webProfileStaging(profile: WebProfile) = when (profile) {
        WebProfile.PHASE1 -> Staging(CapabilityStage.DISABLED, CapabilityStage.DISABLED, CapabilityStage.DISABLED, CapabilityStage.DISABLED)
        WebProfile.PHASE3_ALPHA -> webPhase3Staging()
        WebProfile.PHASE3_BETA -> Staging(CapabilityStage.BETA, CapabilityStage.BETA, CapabilityStage.BETA, CapabilityStage.DISABLED)
        WebProfile.PHASE3_FULL, WebProfile.PHASE4_FULL -> webPhase4Staging()
    }

    @JvmStatic fun applyWebFeatureRollbacks(base: Features, disableAll: Boolean, disableMultiplayer: Boolean, disableFileChooser: Boolean, disableModDownloads: Boolean) =
        if (disableAll) base.copy(onlineMultiplayer = false, customFileChooser = false, onlineModDownloads = false, systemFontEnumeration = false)
        else base.copy(
            onlineMultiplayer = base.onlineMultiplayer && !disableMultiplayer,
            customFileChooser = base.customFileChooser && !disableFileChooser,
            onlineModDownloads = base.onlineModDownloads && !disableModDownloads,
        )

    @JvmStatic fun applyWebStagingRollbacks(base: Staging, disableAll: Boolean, disableMultiplayer: Boolean, disableFileChooser: Boolean, disableModDownloads: Boolean) =
        if (disableAll) base.copy(onlineMultiplayer = CapabilityStage.DISABLED, customFileChooser = CapabilityStage.DISABLED, onlineModDownloads = CapabilityStage.DISABLED, systemFontEnumeration = CapabilityStage.DISABLED)
        else base.copy(
            onlineMultiplayer = if (disableMultiplayer) CapabilityStage.DISABLED else base.onlineMultiplayer,
            customFileChooser = if (disableFileChooser) CapabilityStage.DISABLED else base.customFileChooser,
            onlineModDownloads = if (disableModDownloads) CapabilityStage.DISABLED else base.onlineModDownloads,
        )

    @JvmStatic fun hasWebRollbacksApplied(a: Boolean, b: Boolean, c: Boolean, d: Boolean) = a || b || c || d
    @JvmStatic fun describeWebRollbacks(a: Boolean, b: Boolean, c: Boolean, d: Boolean) = listOfNotNull(if (a) "all" else null, if (b) "multiplayer" else null, if (c) "fileChooser" else null, if (d) "mods" else null).ifEmpty { listOf("none") }.joinToString(",")

    @JvmStatic fun profileFromLabel(raw: String?): WebProfile? = when (raw?.trim()?.lowercase()) {
        "phase1" -> WebProfile.PHASE1
        "phase3-alpha", "phase3_alpha", "phase4-alpha", "phase4_alpha", "alpha" -> WebProfile.PHASE3_ALPHA
        "phase3-beta", "phase3_beta", "phase4-beta", "phase4_beta", "beta" -> WebProfile.PHASE3_BETA
        "phase3-full", "phase3_full", "full" -> WebProfile.PHASE3_FULL
        "phase4-full", "phase4_full", "phase4", "prod", "production" -> WebProfile.PHASE4_FULL
        else -> null
    }
}
