import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

tasks.register<Exec>("webServeDist") {
    dependsOn("webBuildWasm")
    group = "web"
    description = "Serve web/build/dist on http://0.0.0.0:8080"
    workingDir = rootProject.projectDir
    commandLine("python3", "-m", "http.server", "8080", "--directory", "web/build/dist")
}
