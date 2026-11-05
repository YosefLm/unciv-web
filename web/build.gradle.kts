import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kotlin")
    id("com.github.xpenatan.gdx-teavm")
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
