/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.infrastructure.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import com.teammoeg.frostedheart.content.health.nutrition.PlayerNutritionState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** File-backed and remote configurations use the same literal percentage-point rates. */
@GameTestHolder("frostedheart_nutrition_config")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NutritionConfigGameTests {
    private static final List<String> SECTION = List.of("Water & Nutrition");
    private static volatile ModConfig serverConfig;

    @SubscribeEvent
    public static void captureServerConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == FHConfig.SERVER_CONFIG) serverConfig = event.getConfig();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void ratesStayLiteralAcrossConcurrentReloadsAndPreserveGameplay(GameTestHelper helper) throws Exception {
        ModConfig config = serverConfig;
        helper.assertTrue(config != null, "the real server configuration was loaded");
        helper.assertTrue(FHConfig.SERVER.NUTRITION.nutritionGainRate.getDefault() == 1.0D
                        && FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.getDefault() == 0.25D,
                "source defaults already use the runtime percentage units");
        helper.assertTrue(!FHConfig.SERVER_CONFIG.getValues().contains(List.of("Water & Nutrition", "nutritionScaleVersion")),
                "there is no nutrition configuration version setting");
        CommentedConfig original = config.getConfigData();
        Path output = Path.of("nutrition-fix");
        Files.createDirectories(output);
        List<Object> observations = new ArrayList<>();
        observations.add(Map.of("case", "world-startup", "gain", FHConfig.SERVER.NUTRITION.nutritionGainRate.get(),
                "consumption", FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.get()));
        try {
            double[][] settings = {{1.0D, 0.25D}, {0.12D, 0.03D}, {2.75D, 0.17D}, {0, 0}, {0.0003D, 0.0003D}};
            for (int index = 0; index < settings.length; index++) {
                double gain = settings[index][0];
                double consumption = settings[index][1];
                Path path = output.resolve("rates-" + index + ".toml");
                seed(path, original, gain, consumption);
                try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().autosave().build()) {
                    file.load();
                    bind(config, file);
                    var workers = Executors.newFixedThreadPool(2);
                    try {
                        CountDownLatch start = new CountDownLatch(1);
                        var loading = workers.submit(() -> {
                            await(start);
                            FHConfig.onConfigLoading(new ModConfigEvent.Loading(config));
                        });
                        var reloading = workers.submit(() -> {
                            await(start);
                            FHConfig.onConfigReloading(new ModConfigEvent.Reloading(config));
                        });
                        start.countDown();
                        loading.get(5, TimeUnit.SECONDS);
                        reloading.get(5, TimeUnit.SECONDS);
                        for (int reload = 0; reload < 4; reload++) {
                            file.load();
                            FHConfig.SERVER_CONFIG.afterReload();
                            FHConfig.onConfigReloading(new ModConfigEvent.Reloading(config));
                        }
                        assertRates(helper, file, gain, consumption);
                        helper.assertTrue(close(gain, FHConfig.SERVER.NUTRITION.nutritionGainRate.get())
                                        && close(consumption, FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.get()),
                                "runtime rates equal the configured values, including small custom rates and zero");
                        helper.assertTrue(!file.contains(List.of("Water & Nutrition", "nutritionScaleVersion")),
                                "no version marker is generated on disk");
                        observations.add(Map.of("gain", gain, "consumption", consumption, "repeatedReloads", 4));
                    } finally {
                        workers.shutdownNow();
                        workers.awaitTermination(5, TimeUnit.SECONDS);
                        bind(config, original);
                    }
                }
            }
            // Compare the same inputs with the latest, correctly converted historical defaults.
            FoodNutritionProfile food = new FoodNutritionProfile(40, 0, 60, 0);
            PlayerNutritionState initial = PlayerNutritionState.uniform(15);
            PlayerNutritionState sourceMeal = initial.afterEating(food, 8, 12, 1.0D);
            PlayerNutritionState packMeal = initial.afterEating(food, 8, 12, 0.12D);
            helper.assertTrue(Math.abs(sourceMeal.protein() - 19.8F) < 1e-5F
                            && Math.abs(packMeal.protein() - 15.576F) < 1e-5F,
                    "source and pack gains equal the latest once-converted behavior");
            helper.assertTrue(initial.afterHungerLoss(1, 0.25D).equals(PlayerNutritionState.uniform(14.75F))
                            && Math.abs(initial.afterHungerLoss(1, 0.03D).protein() - 14.97F) < 1e-5F,
                    "source and pack hunger consumption retain their previous values");
            helper.assertTrue(initial.afterEating(food, 8, 18, 0.12D).protein()
                            == initial.afterEating(food, 2, 18, 0.12D).protein(),
                    "hunger overflow still contributes no nutrition");
            observations.add(Map.of("case", "gameplay-values-preserved", "sourceMeal", sourceMeal,
                    "packMeal", packMeal, "sourceAfterOneHunger", initial.afterHungerLoss(1, 0.25D),
                    "packAfterOneHunger", initial.afterHungerLoss(1, 0.03D)));
            CommentedConfig remote = CommentedConfig.copy(original);
            CommentedConfig remoteSection = CommentedConfig.copy((CommentedConfig) original.get(SECTION));
            remoteSection.set("nutritionGainRate", 0.12D);
            remoteSection.set("nutritionConsumptionRate", 0.03D);
            remote.set(SECTION, remoteSection);
            bind(config, remote);
            FHConfig.onConfigLoading(new ModConfigEvent.Loading(config));
            FHConfig.onConfigReloading(new ModConfigEvent.Reloading(config));
            assertRates(helper, remote, 0.12D, 0.03D);
            observations.add(Map.of("case", "remote-memory-config-unchanged"));
        } finally {
            bind(config, original);
        }
        Files.writeString(output.resolve("config-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(observations));
        FHMain.LOGGER.info("NUTRITION_CONFIG_REGRESSION,{}", new GsonBuilder().create().toJson(observations));
        helper.succeed();
    }

    private static void seed(Path path, CommentedConfig original, double gain, double consumption) {
        try (CommentedFileConfig seed = CommentedFileConfig.of(path)) {
            seed.putAll(original);
            seed.putAllComments(original);
            CommentedConfig section = CommentedConfig.copy((CommentedConfig) original.get(SECTION));
            section.set("nutritionGainRate", gain);
            section.set("nutritionConsumptionRate", consumption);
            seed.set(SECTION, section);
            seed.save();
        }
    }

    private static void bind(ModConfig config, CommentedConfig data) throws Exception {
        // Forge keeps this binding package-private; use its real acceptConfig path in the fixture.
        var method = ModConfig.class.getDeclaredMethod("setConfigData", CommentedConfig.class);
        method.setAccessible(true);
        method.invoke(config, data);
    }

    private static void await(CountDownLatch start) {
        try { start.await(); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new RuntimeException(failure); }
    }

    private static void assertRates(GameTestHelper helper, CommentedConfig data, double gain, double consumption) {
        CommentedConfig section = data.get(SECTION);
        helper.assertTrue(close(section.<Number>get("nutritionGainRate").doubleValue(), gain)
                        && close(section.<Number>get("nutritionConsumptionRate").doubleValue(), consumption),
                "persisted rates are unchanged");
    }

    private static boolean close(double a, double b) { return Math.abs(a - b) < 1e-12D; }
}
