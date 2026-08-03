package com.unciv.app.web;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.builder.TeaBuilder;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import org.teavm.vm.TeaVMOptimizationLevel;

final class BuildWebCommon {
    private static final String OUTPUT_NAME = "unciv";
    private static final List<String> REFLECTION_PREFIXES = List.of("com.badlogic.gdx.scenes.scene2d");

    private BuildWebCommon() {
    }

    static void build(boolean wasm) {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        Path assetsPath = repoRoot.resolve("android/assets");
        Path outputPath = repoRoot.resolve("web/build/dist");
        Path webappPath = outputPath.resolve("webapp");
        Path resourcesPath = repoRoot.resolve("web/build/resources/main");

        cleanupOutput(outputPath);
        ensureDirectory(resourcesPath);

        WebBackend backend = new WebBackend()
                .setWebAssembly(wasm)
                .setHtmlTitle("Unciv")
                .setHtmlWidth(0)
                .setHtmlHeight(0)
                .setGenerateIndexHtml(true)
                .setCopyAssets(true)
                .setCopyLoadingAsset(false)
                .setWebappFolderName("webapp")
                .setStartJettyAfterBuild(false);
        TeaBuilder builder = new TeaBuilder(backend)
                .addAssets(new AssetFileHandle(assetsPath.toString()));
        builder.setMainClass(WebLauncher.class.getName())
                .setOutputName(OUTPUT_NAME)
                .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
                .setObfuscated(false);
        if (!wasm) {
            builder.setDebugInformationGenerated(true)
                    .setSourceMapsFileGenerated(true);
        }

        builder.build(outputPath.toFile());
        flattenWebapp(webappPath, outputPath);
        ensureStartupLogo(assetsPath, outputPath);
    }

    private static void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed creating directory " + path, e);
        }
    }

    private static void cleanupOutput(Path outputPath) {
        if (!Files.exists(outputPath)) return;
        deleteRecursively(outputPath);
    }

    private static void flattenWebapp(Path webappPath, Path outputPath) {
        if (!Files.isDirectory(webappPath)) {
            throw new IllegalStateException("TeaVM output directory not found: " + webappPath);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(webappPath)) {
            for (Path child : stream) {
                Path target = outputPath.resolve(child.getFileName());
                if (Files.exists(target)) {
                    deleteRecursively(target);
                }
                Files.move(child, target);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to flatten TeaVM webapp directory", e);
        }
        deleteRecursively(webappPath);
    }

    /**
     * The current TeamVM web backend requests this conventional preload image before
     * the application starts. Keep the compatibility asset web-only and derive it
     * from the checked-in Unciv icon so JVM/desktop resource behavior is unchanged.
     */
    private static void ensureStartupLogo(Path assetsPath, Path outputPath) {
        Path source = assetsPath.resolve("ExtraImages/Icons/Unciv32.png");
        Path target = outputPath.resolve("assets/startup-logo.png");
        if (Files.isRegularFile(target) || !Files.isRegularFile(source)) return;
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target);
        } catch (IOException e) {
            throw new RuntimeException("Failed creating web startup logo", e);
        }
    }

    private static void deleteRecursively(Path path) {
        try {
            if (!Files.exists(path)) return;
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed deleting " + current, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed deleting path " + path, e);
        }
    }
}
