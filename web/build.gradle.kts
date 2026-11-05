import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    id("kotlin")
    id("com.github.xpenatan.gdx-teavm") version "1.6.2"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core"))
    implementation(rootProject.libs.kotlin.reflect)
    implementation(rootProject.libs.gdx)
    implementation(rootProject.libs.coroutines.core)
}

providers.gradleProperty("teavmCompatibilityJar").orNull?.let { configuredPath ->
    val compatibilityJar = File(configuredPath)
    if (!compatibilityJar.isAbsolute) {
        throw GradleException("-PteavmCompatibilityJar must be an absolute path: $configuredPath")
    }
    if (!compatibilityJar.isFile || !compatibilityJar.extension.equals("jar", ignoreCase = true)) {
        throw GradleException("-PteavmCompatibilityJar must point to an existing JAR file: $configuredPath")
    }

    // The gdx-teavm plugin adds this configuration to TeaVM's compiler target classpath;
    // keeping the overlay here avoids changing Unciv's normal JVM compile/runtime classpaths.
    dependencies.add("teavm", files(compatibilityJar))
}

gdxTeaVM {
    assets(rootProject.file("android/assets"))

    webDefaults {
        mainClass.set("com.unciv.app.web.WebLauncher")
        htmlTitle.set("Unciv")
        processMemory.set(4096)
    }

    js {
        strict.set(true)
        sourceMap.set(true)
        obfuscated.set(false)
    }
}
