package com.unciv.app.web;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.builder.TeaBuilder;
import com.github.xpenatan.gdx.teavm.backends.shared.config.plugin.TeaReflectionSupplier;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.vm.TeaVMOptimizationLevel;

final class BuildWebCommon {
    private static final String OUTPUT_NAME = "unciv";
    private static final List<String> REFLECTION_PREFIXES = List.of("com.badlogic.gdx.scenes.scene2d");
    private static final List<String> PRESERVED_CLASSES = List.of(
            "com.badlogic.gdx.scenes.scene2d.ui.Skin",
            "com.unciv.models.stats.NamedStats",
            "com.unciv.models.ruleset.RulesetObject",
            "com.unciv.models.ruleset.RulesetStatsObject",
            "com.unciv.models.ruleset.ModOptions",
            "com.unciv.models.ruleset.TechColumn",
            "com.unciv.models.ruleset.tech.Technology",
            "com.unciv.models.ruleset.Building",
            "com.unciv.models.ruleset.tile.Terrain",
            "com.unciv.models.ruleset.tile.TileResource",
            "com.unciv.models.ruleset.tile.TileImprovement",
            "com.unciv.models.ruleset.tech.Era",
            "com.unciv.models.ruleset.Speed",
            "com.unciv.models.ruleset.unit.UnitType",
            "com.unciv.models.ruleset.unit.BaseUnit",
            "com.unciv.models.ruleset.unit.Promotion",
            "com.unciv.models.ruleset.unit.UnitNameGroup",
            "com.unciv.models.ruleset.Quest",
            "com.unciv.models.ruleset.Specialist",
            "com.unciv.models.ruleset.PolicyBranch",
            "com.unciv.models.ruleset.Policy",
            "com.unciv.models.ruleset.Belief",
            "com.unciv.models.ruleset.RuinReward",
            "com.unciv.models.ruleset.nation.Nation",
            "com.unciv.models.ruleset.nation.Difficulty",
            "com.unciv.models.ruleset.GlobalUniques",
            "com.unciv.models.ruleset.Victory",
            "com.unciv.models.ruleset.nation.CityStateType",
            "com.unciv.models.ruleset.nation.Personality",
            "com.unciv.models.ruleset.Event",
            "com.unciv.models.ruleset.EventChoice",
            "com.unciv.models.ruleset.Tutorial",
            "com.unciv.logic.GameInfo",
            "com.unciv.logic.VictoryData",
            "com.unciv.logic.map.TileMap",
            "com.unciv.logic.map.tile.Tile",
            "com.unciv.logic.map.mapunit.MapUnit",
            "com.unciv.logic.map.mapunit.MapUnit$UnitMovementMemory",
            "com.unciv.logic.map.mapunit.UnitPromotions",
            "com.unciv.logic.city.City",
            "com.unciv.logic.city.CityConstructions",
            "com.unciv.logic.civilization.Civilization",
            "com.unciv.logic.civilization.Civilization$NotificationsLog",
            "com.unciv.logic.civilization.Civilization$HistoricalAttackMemory",
            "com.unciv.logic.civilization.Notification",
            "com.unciv.logic.civilization.PopupAlert",
            "com.unciv.logic.civilization.ExploredRegion",
            "com.unciv.logic.civilization.CivRankingHistory",
            "com.unciv.logic.civilization.managers.VictoryManager",
            "com.unciv.logic.civilization.managers.EspionageManager",
            "com.unciv.logic.civilization.managers.ReligionManager",
            "com.unciv.logic.civilization.managers.QuestManager",
            "com.unciv.logic.civilization.managers.AssignedQuest",
            "com.unciv.logic.civilization.managers.TechManager",
            "com.unciv.logic.civilization.managers.GoldenAgeManager",
            "com.unciv.logic.civilization.managers.PolicyManager",
            "com.unciv.logic.civilization.managers.GreatPersonManager",
            "com.unciv.logic.civilization.CivConstructions",
            "com.unciv.logic.civilization.diplomacy.DiplomacyManager",
            "com.unciv.logic.city.managers.CityPopulationManager",
            "com.unciv.logic.city.managers.CityReligionManager",
            "com.unciv.logic.city.managers.CityEspionageManager",
            "com.unciv.logic.city.managers.CityExpansionManager",
            "com.unciv.logic.trade.Trade",
            "com.unciv.logic.trade.TradeRequest",
            "com.unciv.logic.trade.TradeOffer",
            "com.unciv.logic.trade.TradeOffersList",
            "com.unciv.logic.automation.civilization.BarbarianManager",
            "com.unciv.logic.automation.civilization.Encampment",
            "com.unciv.models.Religion",
            "com.unciv.models.Spy",
            "com.unciv.models.ruleset.unique.TemporaryUnique",
            "com.unciv.models.metadata.GameParameters",
            "com.unciv.logic.map.MapParameters",
            "com.unciv.ui.screens.pickerscreens.PromotionScreenColors");

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
        Set<String> reflectionClasses = new LinkedHashSet<>(PRESERVED_CLASSES);
        for (String className : reflectionClasses) {
            if (!TeaReflectionSupplier.containsReflection(className)) {
                TeaReflectionSupplier.addReflectionClass(className);
            }
        }
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
