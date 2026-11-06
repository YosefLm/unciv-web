import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.teavm.gradle.tasks.GenerateJavaScriptTask

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.teavm") version "0.16.0"
}

repositories { mavenCentral() }

dependencies {
    implementation("io.ktor:ktor-client-core:3.5.2")
    implementation("org.teavm:teavm-jso-apis:0.16.0")
    teavm("io.ktor:ktor-client-core:3.5.2")
    teavm("org.teavm:teavm-jso-apis:0.16.0")
}

kotlin { jvmToolchain(21) }

sourceSets.main {
    java.srcDir("src/main/kotlin")
}

tasks.named<ProcessResources>("processResources") {
    from(file("../../src/main/resources/META-INF/services/io.ktor.client.HttpClientEngineContainer")) {
        into("META-INF/services")
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    source("../../src/main/kotlin/com/unciv/app/web/FetchHttpClientEngine.kt")
}

teavm.js {
    mainClass = "validation.MainKt"
    obfuscated = false
    strict = true
}

tasks.named<GenerateJavaScriptTask>("generateJavaScript") {
    val classlibPath = providers.gradleProperty("classlibJar").orNull
        ?: throw GradleException("Pass -PclasslibJar=<TeaVM 0.16 classlib jar>")
    val classlibJar = file(classlibPath)
    require(classlibJar.isFile) { "TeaVM classlib jar does not exist: $classlibPath" }

    val originalTarget = objects.fileCollection().from(classpath.from.toList())
    val target = originalTarget.filter { it.name != "teavm-classlib-0.16.0.jar" }
    classpath.setFrom(target, classlibJar)
    daemonClasspath.from(target.filter { it.name == "teavm-platform-0.16.0.jar" })

    val corePath = providers.gradleProperty("coreJar").orNull
        ?: throw GradleException("Pass -PcoreJar=<matching TeaVM 0.16 core jar>")
    val forkCore = file(corePath)
    require(forkCore.isFile) { "TeaVM core jar does not exist: $corePath" }
    val originalDaemon = objects.fileCollection().from(daemonClasspath.from.toList())
    daemonClasspath.setFrom(originalDaemon.filter { it.name != "teavm-core-0.16.0.jar" }, forkCore)
    outOfProcess.set(true)
}

tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
