import java.util.Properties

pluginManagement {
    repositories {
        mavenLocal() // To get the compiler plugin locally
        gradlePluginPortal() // So other plugins can be resolved
    }
}

include("desktop", "core", "tests", "server")

// Browser support is experimental and opt-in so it does not affect the normal build.
if (providers.gradleProperty("web").isPresent) include("web")

// Optional source override for the published browser backend. This is for local port development
// only; the default build always resolves the pinned Maven Central release.
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
