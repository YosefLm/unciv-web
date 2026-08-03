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
val generatedWebJsTestsDir = layout.buildDirectory.dir("generated/web-jstests/kotlin")

sourceSets {
    main {
        java.srcDir("src/main/java")
        java.srcDir("src/main/kotlin")
        java.srcDir("../tests/src")
        java.srcDir(generatedWebJsTestsDir)
        java.exclude("com/unciv/dev/**")
        // This test imports desktop-only editor classes and is covered by the
        // JVM test source set, not by the browser-generated suite.
        java.exclude("com/unciv/ui/components/tilegroups/EditorMapHolderMemoryTest.kt")
        java.exclude("com/unciv/testing/GdxTestRunner.kt")
        java.exclude("com/unciv/testing/GdxTestRunnerFactory.kt")
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
    implementation(kotlin("reflect"))
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

// These tests remain part of the exact upstream JVM gate.  TeaVM's browser
// reflection cannot execute their JVM-only assumptions (cyclic GDX field
// metadata, Java annotation lookup, or lazy delegate construction).  Keep the
// exclusions explicit and method-level so a newly added browser test can never
// disappear silently; the web E2E flows cover the corresponding save, ruleset,
// and gameplay behavior.
val webUnsupportedTestReasons = mapOf(
    "com.unciv.logic.GameSerializationTests.canSerializeGame" to "TeaVM GDX reflection cannot serialize cyclic JVM transient metadata",
    "com.unciv.logic.GameSerializationTests.serializedLaziesTest" to "TeaVM GDX reflection cannot inspect JVM lazy delegate metadata",
    "com.unciv.logic.GameSerializationTests.checksumUsesTeavmSafeFallbackWithoutBackgroundPools" to "TeaVM GDX reflection cannot serialize the full JVM GameInfo graph",
    "com.unciv.logic.civilization.QuestTests.testSerializeQuestManager" to "TeaVM GDX reflection cannot serialize the cyclic quest manager graph",
    "com.unciv.uniques.CountableTests.testCountableConventions" to "TeaVM enum reflection does not expose the JVM deprecation metadata used by this validator test",
    "com.unciv.uniques.CountableTests.testRulesetValidation" to "TeaVM enum reflection does not expose the JVM deprecation metadata used by this validator test",
    "com.unciv.uniques.DeprecatedUniquesTest.allDeprecatedUniqueTypeReplacementChainsTerminate" to "TeaVM Java annotation reflection cannot read Deprecated.ReplaceWith",
    "com.unciv.uniques.DeprecatedUniquesTest.fullyDeprecatedUniqueReportsErrorInValidator" to "TeaVM Java annotation reflection cannot read Deprecated.ReplaceWith",
    "com.unciv.uniques.DeprecatedUniquesTest.halfDeprecatedUniqueReportsWarningInValidator" to "TeaVM Java annotation reflection cannot read Deprecated.ReplaceWith",
    "com.unciv.uniques.DeprecatedUniquesTest.autoUpdaterReplacesFullyDeprecatedUnique" to "TeaVM Java annotation reflection cannot read Deprecated.ReplaceWith",
    "com.unciv.uniques.DeprecatedUniquesTest.autoUpdaterFollowsChainThroughDeprecatedUniqueType" to "TeaVM Java annotation reflection cannot read Deprecated.ReplaceWith",
    "com.unciv.uniques.DeprecatedUniquesTest.autoUpdaterHandlesMixOfHalfAndFullyDeprecatedUniques" to "TeaVM Java annotation reflection cannot read Deprecated.ReplaceWith",
    "com.unciv.uniques.UnitUniquesTests.canConstructResourceRequiringImprovement" to "TeaVM GDX reflection cannot construct Kotlin lazy delegate metadata",
)

val generateWebJsTestSuite by tasks.registering {
    group = "web"
    description = "Generate a browser-invokable suite for zero-argument JVM tests."
    val testsSourceRoot = rootProject.file("tests/src")
    inputs.dir(testsSourceRoot)
    outputs.dir(generatedWebJsTestsDir)

    doLast {
        val outputFile = generatedWebJsTestsDir.get().asFile.resolve("com/unciv/app/web/WebJsTestSuite.kt")
        outputFile.parentFile.mkdirs()
        val packageRegex = Regex("^\\s*package\\s+([A-Za-z0-9_.]+)", setOf(RegexOption.MULTILINE))
        val classRegex = Regex("^\\s*(?:public\\s+)?(?:class|object)\\s+([A-Za-z0-9_]+)\\s*(?:\\(([^)]*)\\))?", setOf(RegexOption.MULTILINE))
        val methodRegex = Regex("^\\s*(?:public\\s+|private\\s+|internal\\s+|protected\\s+)?(?:suspend\\s+)?fun\\s+([A-Za-z0-9_]+)\\s*\\(")
        val candidates = testsSourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/com/unciv/dev/") }
            .filterNot { it.name == "EditorMapHolderMemoryTest.kt" }
            .filterNot { it.name == "LongPriorityQueueTest.kt" }
            .sortedBy { it.invariantSeparatorsPath }
            .toList()

        val body = buildString {
            appendLine("package com.unciv.app.web")
            appendLine()
            appendLine("data class WebJsGeneratedTestMethod(val name: String, val ignoredReason: String?, val execute: (Any) -> Unit)")
            appendLine("data class WebJsGeneratedTestClass(val className: String, val createInstance: () -> Any, val beforeMethods: List<(Any) -> Unit>, val afterMethods: List<(Any) -> Unit>, val testMethods: List<WebJsGeneratedTestMethod>)")
            appendLine("object WebJsTestSuite {")
            appendLine("    val classes: List<WebJsGeneratedTestClass> = listOf(")
            var classCount = 0
            for (file in candidates) {
                val source = file.readText()
                if (!source.contains("@Test")) continue
                val classMatch = classRegex.find(source) ?: continue
                if (!classMatch.groupValues.getOrNull(2).orEmpty().trim().isEmpty()) continue
                val packageName = packageRegex.find(source)?.groupValues?.get(1) ?: continue
                val className = classMatch.groupValues[1]
                val pending = mutableListOf<String>()
                val beforeMethods = mutableListOf<String>()
                val afterMethods = mutableListOf<String>()
                val testMethods = mutableListOf<Pair<String, Boolean>>()
                for (rawLine in source.lineSequence()) {
                    val line = rawLine.trim()
                    if (line.contains("@Before")) pending += "before"
                    if (line.contains("@After")) pending += "after"
                    if (line.contains("@Test")) pending += "test"
                    if (line.contains("@Ignore")) pending += "ignore"
                    val method = methodRegex.find(line)?.groupValues?.get(1)
                    if (method != null) {
                        if ("before" in pending) beforeMethods += method
                        if ("after" in pending) afterMethods += method
                        if ("test" in pending) testMethods += method to ("ignore" in pending)
                        pending.clear()
                    } else if (line.isNotBlank() && !line.startsWith("@") && !line.startsWith("//")) {
                        pending.clear()
                    }
                }
                if (testMethods.isEmpty()) continue
                classCount++
                val fqn = "$packageName.$className"
                appendLine("        WebJsGeneratedTestClass(")
                appendLine("            className = \"$fqn\",")
                appendLine("            createInstance = { $fqn() },")
                val beforeCode = beforeMethods.joinToString(", ") { method -> "{ instance -> (instance as $fqn).$method() }" }
                val afterCode = afterMethods.joinToString(", ") { method -> "{ instance -> (instance as $fqn).$method() }" }
                appendLine("            beforeMethods = listOf($beforeCode),")
                appendLine("            afterMethods = listOf($afterCode),")
                appendLine("            testMethods = listOf(")
                for ((method, ignored) in testMethods) {
                    val explicitReason = webUnsupportedTestReasons["$fqn.$method"]
                    val ignoredValue = when {
                        ignored -> "\"ignored\""
                        explicitReason != null -> "\"${explicitReason.replace("\"", "\\\"")}\""
                        else -> "null"
                    }
                    appendLine("                WebJsGeneratedTestMethod(\"$method\", $ignoredValue, { instance -> (instance as $fqn).$method() }),")
                }
                appendLine("            ),")
                appendLine("        ),")
            }
            appendLine("    )")
            appendLine("}")
            logger.lifecycle("Generated $classCount zero-argument browser test classes at ${outputFile.invariantSeparatorsPath}")
        }
        outputFile.writeText(body)
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateWebJsTestSuite)
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
    if (!content.contains("rel=\"icon\"")) content = content.replace("<head>", "<head>\n        <link rel=\"icon\" href=\"data:,\">")
    val hardened = """
        <script>
            (function () {
                function boot() {
                    if (window.__uncivBootStarted) return;
                    if (typeof window.main !== 'function') { setTimeout(boot, 25); return; }
                    window.__uncivBootStarted = true;
                    window.main();
                }
                if (document.readyState === 'complete') setTimeout(boot, 0);
                else window.addEventListener('load', boot, { once: true });
            })();
        </script>
    """.trimIndent()
    val legacy = Regex("<script>\\s*async function start\\(\\).*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    content = if (legacy.containsMatchIn(content)) content.replace(legacy, hardened)
        else if (!content.contains("__uncivBootStarted")) content.replace("</body>", "$hardened\n    </body>") else content
    indexFile.writeText(content)
}

fun promoteWebappToRoot(outputDir: File) {
    val webappDir = File(outputDir, "webapp")
    if (!webappDir.isDirectory || File(outputDir, "index.html").isFile) return
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

tasks.register<JavaExec>("webGenerateWarPreloads") {
    dependsOn("classes")
    group = "web"
    description = "Generate deterministic WAR UI preload fixtures."
    mainClass.set("com.unciv.app.web.WebWarPreloadTool")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
    args("generate")
    maxHeapSize = "2g"
    jvmArgs("-Xms512m", "-XX:+UseG1GC")
}

tasks.register<JavaExec>("webVerifyWarPreloads") {
    dependsOn("classes")
    group = "web"
    description = "Verify deterministic WAR UI preload fixtures."
    mainClass.set("com.unciv.app.web.WebWarPreloadTool")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
    args("verify")
    maxHeapSize = "2g"
    jvmArgs("-Xms512m", "-XX:+UseG1GC")
}

tasks.named("webBuildJs") {
    dependsOn("webVerifyWarPreloads")
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
