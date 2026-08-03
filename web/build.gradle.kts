import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("kotlin")
}

val gdxVersion = rootProject.libs.versions.gdx.get()
val coroutinesVersion = rootProject.libs.versions.coroutines.get()
val ktorVersion = rootProject.libs.versions.ktor.get()
val gdxTeaVMVersion = "-SNAPSHOT"

sourceSets {
    main {
        java.srcDir("src/main/java")
        java.srcDir("src/main/kotlin")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    maven { url = uri("https://jitpack.io") }
    maven {
        url = uri("http://teavm.org/maven/repository/")
        isAllowInsecureProtocol = true
    }
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
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("com.github.xpenatan.gdx-teavm:backend-web:$gdxTeaVMVersion")
    implementation("com.github.xpenatan.gdx-teavm:gdx-freetype-web:$gdxTeaVMVersion")
}

// Keep the first independently built web branch on the API surface modeled by
// the current TeamVM snapshot. This is web-only dependency resolution; the
// upstream JVM/Android graphs continue to use their declared versions.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines-")) {
            useVersion("1.8.1")
            because("TeamVM 0.15 does not model the newer coroutine scheduler JDK methods")
        }
        if (requested.group == "com.badlogicgames.gdx" && requested.name == "gdx") {
            useVersion(gdxVersion)
            because("The web graph must use the same GDX ABI as the upstream core")
        }
    }
}

tasks.register<JavaExec>("webBuildWasm") {
    dependsOn("classes")
    group = "web"
    description = "Build WASM web bundle with TeaVM"
    mainClass.set("com.unciv.app.web.BuildWebWasm")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
    // The latest TeamVM snapshot performs a large first-time dependency
    // analysis even on the minimal bootstrap branch.
    maxHeapSize = "8g"
    jvmArgs("-Xms1g", "-XX:+UseG1GC")
}

fun hardenIndexBootstrap(indexFile: File) {
    if (!indexFile.isFile) return
    var content = indexFile.readText()
    if (!content.contains("rel=\"icon\"")) {
        content = content.replace("<head>", "<head>\n        <link rel=\"icon\" href=\"data:,\">")
    }
    val hardened = """
        <script>
            (function () {
                function boot() {
                    if (window.__uncivBootStarted) return;
                    if (typeof window.main !== 'function') {
                        setTimeout(boot, 25);
                        return;
                    }
                    window.__uncivBootStarted = true;
                    window.main();
                }
                if (document.readyState === 'complete') {
                    setTimeout(boot, 0);
                } else {
                    window.addEventListener('load', boot, { once: true });
                }
            })();
        </script>
    """.trimIndent()
    val legacyRegex = Regex(
        "<script>\\s*async function start\\(\\) \\{\\s*main\\(\\)\\s*}\\s*window.addEventListener\\(\\\"load\\\", start\\);\\s*</script>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    content = if (legacyRegex.containsMatchIn(content)) {
        content.replace(legacyRegex, hardened)
    } else if (!content.contains("__uncivBootStarted")) {
        content.replace("</body>", "$hardened\n    </body>")
    } else {
        content
    }
    indexFile.writeText(content)
}

fun promoteWebappToRoot(outputDir: File) {
    val webappDir = File(outputDir, "webapp")
    if (!webappDir.isDirectory) return
    if (File(outputDir, "index.html").isFile) return
    webappDir.listFiles()?.forEach { child ->
        val target = File(outputDir, child.name)
        if (target.exists()) target.deleteRecursively()
        Files.move(child.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    webappDir.deleteRecursively()
}

tasks.register("webPostProcessDist") {
    doLast {
        val outputDir = rootProject.file("web/build/dist")
        promoteWebappToRoot(outputDir)
        hardenIndexBootstrap(File(outputDir, "index.html"))
    }
}

tasks.register<JavaExec>("webBuildJs") {
    dependsOn("classes")
    group = "web"
    description = "Build JS web bundle with TeaVM"
    mainClass.set("com.unciv.app.web.BuildWebJs")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
    // Keep every independently built stack branch within the same generous
    // TeaVM analysis budget; later branches may reuse the analysis cache.
    maxHeapSize = "8g"
    jvmArgs("-Xms1g", "-XX:+UseG1GC")
}

tasks.named("webBuildJs") {
    finalizedBy("webPostProcessDist")
}

tasks.named("webBuildWasm") {
    finalizedBy("webPostProcessDist")
}

tasks.register<Exec>("webServeDist") {
    dependsOn("webBuildWasm")
    group = "web"
    description = "Serve web/build/dist on http://0.0.0.0:8080"
    workingDir = rootProject.projectDir
    commandLine("python3", "-m", "http.server", "8080", "--directory", "web/build/dist")
}
