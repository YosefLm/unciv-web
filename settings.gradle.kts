import java.util.Properties

pluginManagement {
    val webEnabled = providers.gradleProperty("web").isPresent
    val gdxTeaVMSourcePath = providers.gradleProperty("gdxTeaVMSource").orNull
    if (webEnabled && gdxTeaVMSourcePath == null) {
        throw GradleException("Experimental -Pweb builds require -PgdxTeaVMSource pointing to a gdx-teavm source checkout whose version catalog pins TeaVM 0.16.0")
    }
    if (webEnabled) {
        val configuredPath = checkNotNull(gdxTeaVMSourcePath)
        val sourceCheckout = File(configuredPath)
        if (!sourceCheckout.isAbsolute || !sourceCheckout.isDirectory) {
            throw GradleException("-PgdxTeaVMSource must point to an absolute gdx-teavm source checkout: $configuredPath")
        }
        val pluginCheckout = File(sourceCheckout, "tools/gdx-teavm-plugin")
        if (!File(pluginCheckout, "settings.gradle.kts").isFile) {
            throw GradleException("-PgdxTeaVMSource must contain tools/gdx-teavm-plugin/settings.gradle.kts: $configuredPath")
        }
        val versions = File(sourceCheckout, "gradle/libs.versions.toml").takeIf { it.isFile }
            ?: throw GradleException("-PgdxTeaVMSource is missing gradle/libs.versions.toml: $configuredPath")
        val catalog = versions.readText()
        if (!catalog.contains("gdxTeavmRelease = \"1.7.0\"") || !catalog.contains("teavm = \"0.16.0\"")) {
            throw GradleException("-PgdxTeaVMSource must pin gdx-teavm 1.7.0 / TeaVM 0.16.0: $configuredPath")
        }
        // Use the backend and its plugin from the same source checkout.
        includeBuild(pluginCheckout.absolutePath)
    }

    repositories {
        if (!webEnabled) mavenLocal() // Preserve the existing JVM/Android plugin workflow.
        gradlePluginPortal() // So other plugins can be resolved
    }
}

include("desktop", "core", "tests", "server")

// Browser support is experimental and opt-in so it does not affect the normal build.
if (providers.gradleProperty("web").isPresent) include("web")

// Experimental web builds use an explicit TeaVM 0.16-compatible gdx-teavm checkout.
if (providers.gradleProperty("web").isPresent) {
    providers.gradleProperty("gdxTeaVMSource").orNull?.let { configuredPath ->
        val sourceCheckout = File(configuredPath)
        if (!sourceCheckout.isAbsolute) {
            throw GradleException("-PgdxTeaVMSource must be an absolute path: $configuredPath")
        }
        if (!sourceCheckout.isDirectory ||
            !(File(sourceCheckout, "settings.gradle.kts").isFile || File(sourceCheckout, "settings.gradle").isFile)
        ) {
            throw GradleException("-PgdxTeaVMSource must point to a gdx-teavm checkout containing Gradle settings: $configuredPath")
        }

        includeBuild(sourceCheckout.absolutePath) {
            dependencySubstitution {
                substitute(module("com.github.xpenatan.gdx-teavm:backend-web"))
                    .using(project(":backends:backend-web"))
            }
        }
    }
}

private fun getSdkPath(): String? {
    val localProperties = file("local.properties")
    return if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }

        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
    } else {
        System.getenv("ANDROID_HOME")
    }
}
if (getSdkPath() != null) include("android")
