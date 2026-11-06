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
    compileOnly("org.teavm:teavm-extension-spi:0.16.0")
}

configurations.configureEach {
    exclude(group = "io.ktor", module = "ktor-client-cio")
    exclude(group = "io.ktor", module = "ktor-client-cio-jvm")
}

val validationClasspath = layout.buildDirectory.file("validation/runtime-classpath.txt")
tasks.register("writeValidationClasspath") {
    dependsOn(tasks.named("classes"))
    inputs.files(sourceSets.main.get().runtimeClasspath)
    outputs.file(validationClasspath)
    doLast {
        val output = validationClasspath.get().asFile
        output.parentFile.mkdirs()
        output.writeText(sourceSets.main.get().runtimeClasspath.files.joinToString("\n") { it.absolutePath })
    }
}

gdxTeaVM {
    assets(rootProject.file("android/assets"))

    webDefaults {
        mainClass.set("com.unciv.app.web.WebLauncher")
        htmlTitle.set("Unciv")
        processMemory.set(providers.gradleProperty("webCompilerMemory").map(String::toInt).orElse(4096))
    }

    js {
        strict.set(true)
        sourceMap.set(true)
        obfuscated.set(false)
    }
}
