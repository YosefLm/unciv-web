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
import java.util.Locale;
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
        ensureDirectory(outputPath);
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
        sanitizeAtlasFiltersForWeb(outputPath.resolve("assets"));
        if (!wasm) hardenIndexBootstrap(outputPath.resolve("index.html"));
        ensureFavicon(outputPath);
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

    /**
     * TeaVM/WebGL currently logs GL_INVALID_ENUM for mipmap filter tokens from atlas headers.
     * Rewrite copied atlas files for web output only to avoid noisy warnings.
     */
    private static void sanitizeAtlasFiltersForWeb(Path assetsPath) {
        if (!Files.isDirectory(assetsPath)) return;
        try {
            Files.walk(assetsPath)
                    .filter(path -> Files.isRegularFile(path) && path.toString().toLowerCase(Locale.ROOT).endsWith(".atlas"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            String sanitized = content
                                    .replace("MipMapLinearLinear", "Linear")
                                    .replace("MipMapLinearNearest", "Linear")
                                    .replace("MipMapNearestLinear", "Nearest")
                                    .replace("MipMapNearestNearest", "Nearest");
                            if (!sanitized.equals(content)) {
                                Files.writeString(path, sanitized);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed sanitizing atlas filters for " + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed scanning atlas files in " + assetsPath, e);
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

    private static void ensureFavicon(Path outputPath) {
        Path faviconPath = outputPath.resolve("favicon.ico");
        if (Files.exists(faviconPath)) return;
        try {
            Files.write(faviconPath, new byte[] {0});
        } catch (IOException e) {
            throw new RuntimeException("Failed creating favicon at " + faviconPath, e);
        }
    }

    private static void hardenIndexBootstrap(Path indexPath) {
        if (!Files.isRegularFile(indexPath)) return;
        try {
            String content = Files.readString(indexPath);
            String legacy = "<script>\n"
                    + "            async function start() {\n"
                    + "                main()\n"
                    + "            }\n"
                    + "            window.addEventListener(\"load\", start);\n"
                    + "        </script>";
            String hardened = "<script>\n"
                    + "            (function () {\n"
                    + "                function boot() {\n"
                    + "                    if (window.__uncivBootStarted) return;\n"
                    + "                    if (typeof window.main !== 'function') { setTimeout(boot, 25); return; }\n"
                    + "                    window.__uncivBootStarted = true;\n"
                    + "                    window.main();\n"
                    + "                }\n"
                    + "                if (document.readyState === 'complete') setTimeout(boot, 0);\n"
                    + "                else window.addEventListener('load', boot, { once: true });\n"
                    + "            })();\n"
                    + "        </script>";
            if (content.contains(legacy)) content = content.replace(legacy, hardened);
            else if (!content.contains("__uncivBootStarted")) content = content.replace("</body>", hardened + "\n    </body>");
            Files.writeString(indexPath, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed hardening index bootstrap at " + indexPath, e);
        }
    }
}
