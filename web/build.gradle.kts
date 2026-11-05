import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.teavm.gradle.tasks.GenerateJavaScriptTask
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

providers.gradleProperty("teavmClasslibJar").orNull?.let { configuredPath ->
    if (providers.gradleProperty("teavmCompatibilityJar").isPresent) {
        throw GradleException("Use only one of -PteavmCompatibilityJar or -PteavmClasslibJar")
    }

    val teavmClasslibJar = File(configuredPath)
    if (!teavmClasslibJar.isAbsolute) {
        throw GradleException("-PteavmClasslibJar must be an absolute path: $configuredPath")
    }
    if (!teavmClasslibJar.isFile || !teavmClasslibJar.extension.equals("jar", ignoreCase = true)) {
        throw GradleException("-PteavmClasslibJar must point to an existing JAR file: $configuredPath")
    }

    fun resolvedTeaVMArtifacts(moduleName: String) = configurations.named("teavm").map { configuration ->
        val artifacts = configuration.incoming.artifacts.artifacts.mapNotNull { artifact ->
            val module = artifact.id.componentIdentifier as? ModuleComponentIdentifier
            if (module?.group == "org.teavm" && module.module == moduleName) artifact.file else null
        }.toSet()
        if (artifacts.isEmpty()) {
            throw GradleException("The TeaVM configuration did not resolve org.teavm:$moduleName")
        }
        artifacts
    }
    val teavmClasslibArtifacts = resolvedTeaVMArtifacts("teavm-classlib")
    val classlibArtifacts = files(teavmClasslibArtifacts)
    val teavmPlatformArtifacts = files(resolvedTeaVMArtifacts("teavm-platform"))

    fun replaceClasslibArtifact(original: ConfigurableFileCollection, requireClasslib: Boolean): FileCollection {
        // Copy the `from` entries before resetting the task collection, keeping all unrelated
        // provider-backed inputs and their build dependencies lazy without self-reference.
        val originalCollection = objects.fileCollection().apply {
            setFrom(original.from.toList())
        }
        val classlibEntries = originalCollection.filter { file -> teavmClasslibArtifacts.get().contains(file) }
        val replacement = files(provider {
            if (classlibEntries.files.isEmpty()) {
                if (requireClasslib) {
                    throw GradleException("TeaVM target classpath did not contain its resolved org.teavm:teavm-classlib artifact")
                }
                emptyList()
            } else listOf(teavmClasslibJar)
        })
        return originalCollection.minus(classlibArtifacts).plus(replacement)
    }

    tasks.withType<GenerateJavaScriptTask>().configureEach {
        classpath.setFrom(replaceClasslibArtifact(classpath, requireClasslib = true))
        val compilerDaemonClasspath = replaceClasslibArtifact(daemonClasspath, requireClasslib = false)
        // Out-of-process TeaVM needs teavm-platform's MetadataGenerator while constructing
        // the target VM. This is the only target artifact needed in the daemon classpath.
        daemonClasspath.setFrom(compilerDaemonClasspath, teavmPlatformArtifacts)
    }
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
