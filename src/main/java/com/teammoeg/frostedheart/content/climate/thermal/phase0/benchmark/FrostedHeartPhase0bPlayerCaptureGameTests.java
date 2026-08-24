/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.benchmark;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.data.BlockTempData;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;

/** Forge-backed legacy capture baseline; this is diagnostic evidence, not an acceptance gate. */
@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class FrostedHeartPhase0bPlayerCaptureGameTests {
    private static final String BATCH = "frostedheart_phase0b_player_capture";
    private static final String TEMPLATE = "phase0a_empty";
    private static final String REPORT_PROPERTY = "frostedheart.phase0bPlayerCaptureReport";
    private static final int WARMUP_ITERATIONS = 128;
    private static final int MEASUREMENT_ITERATIONS = 1_024;
    private static volatile SurroundingTemperatureSimulator captureBlackhole;

    private FrostedHeartPhase0bPlayerCaptureGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void capturesRealWorldConstructionAndChunkLoadBehavior(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sample = helper.absolutePos(new BlockPos(10, 6, 10));
        buildRoom(level, sample);

        ChunkPos[] loadedFootprint = captureFootprint(sample);
        preload(level, loadedFootprint);
        int loadedBeforeMeasurement = countLoaded(level, loadedFootprint);
        helper.assertTrue(loadedBeforeMeasurement == loadedFootprint.length,
                "the measured capture footprint must be fully loaded before timing");

        CaptureDistributions captures = measureBoth(level, sample);
        int loadedAfterMeasurement = countLoaded(level, loadedFootprint);
        helper.assertTrue(loadedAfterMeasurement == loadedBeforeMeasurement,
                "capture timing over a loaded footprint must not change its loaded chunk count");

        ChunkPos localAnchor = new ChunkPos(sample);
        ChunkPos remoteAnchor = new ChunkPos(localAnchor.x + 8_192, localAnchor.z + 8_192);
        int range = FHConfig.SERVER.SIMULATION.simulationRange.get();
        BlockPos remoteSample = new BlockPos(
                (remoteAnchor.x << 4) + range,
                sample.getY(),
                (remoteAnchor.z << 4) + range);
        ChunkPos[] remoteFootprint = captureFootprint(remoteSample);
        int remoteLoadedBefore = countLoaded(level, remoteFootprint);
        helper.assertTrue(remoteLoadedBefore == 0,
                "the remote diagnostic footprint must start completely unloaded");

        captureBlackhole = new SurroundingTemperatureSimulator(
                level, remoteSample.getX() + 0.5D, remoteSample.getY(),
                remoteSample.getZ() + 0.5D, true);
        int remoteLoadedAfter = countLoaded(level, remoteFootprint);
        int constructorTriggeredLoads = remoteLoadedAfter - remoteLoadedBefore;
        helper.assertTrue(constructorTriggeredLoads > 0,
                "legacy ServerLevel#getChunk capture was expected to load an absent footprint");

        BlockTempData campfire = BlockTempData.getData(Blocks.CAMPFIRE);
        ResourceLocation campfireRecipe =
                new ResourceLocation(FHMain.MODID, "block_temperature/campfire");
        helper.assertTrue(ForgeRegistries.BLOCKS.getKey(Blocks.CAMPFIRE) != null,
                "Forge block registry must be available to the capture fixture");
        helper.assertTrue(level.getServer().getRecipeManager().byKey(campfireRecipe).isPresent(),
                "the generated campfire block-temperature recipe must be present after reload");
        helper.assertTrue(campfire != null && campfire.temperature() == 500.0F && campfire.lit(),
                "the recipe caching reload listener must populate BlockTempData before capture");

        writeReport(
                level,
                captures.synchronous(),
                captures.asynchronous(),
                loadedBeforeMeasurement,
                loadedAfterMeasurement,
                remoteLoadedBefore,
                remoteLoadedAfter,
                constructorTriggeredLoads,
                campfireRecipe,
                campfire);
        captureBlackhole = null;
        helper.succeed();
    }

    private static CaptureDistributions measureBoth(ServerLevel level, BlockPos sample) {
        double x = sample.getX() + 0.5D;
        double y = sample.getY() + 0.8D;
        double z = sample.getZ() + 0.5D;
        for (int iteration = 0; iteration < WARMUP_ITERATIONS; iteration++) {
            captureBlackhole = new SurroundingTemperatureSimulator(level, x, y, z, false);
            captureBlackhole = new SurroundingTemperatureSimulator(level, x, y, z, true);
        }
        long[] synchronous = new long[MEASUREMENT_ITERATIONS];
        long[] asynchronous = new long[MEASUREMENT_ITERATIONS];
        for (int iteration = 0; iteration < MEASUREMENT_ITERATIONS; iteration++) {
            if ((iteration & 1) == 0) {
                synchronous[iteration] = measureOne(level, x, y, z, false);
                asynchronous[iteration] = measureOne(level, x, y, z, true);
            } else {
                asynchronous[iteration] = measureOne(level, x, y, z, true);
                synchronous[iteration] = measureOne(level, x, y, z, false);
            }
        }
        return new CaptureDistributions(distribution(synchronous), distribution(asynchronous));
    }

    private static long measureOne(
            ServerLevel level,
            double x,
            double y,
            double z,
            boolean threadSafe
    ) {
        long started = System.nanoTime();
        captureBlackhole = new SurroundingTemperatureSimulator(level, x, y, z, threadSafe);
        return System.nanoTime() - started;
    }

    private static CaptureDistribution distribution(long[] samples) {
        Arrays.sort(samples);
        return new CaptureDistribution(
                percentile(samples, 0.50D),
                percentile(samples, 0.95D),
                percentile(samples, 0.99D),
                samples[samples.length - 1]);
    }

    private static long percentile(long[] sortedSamples, double percentile) {
        int index = Math.max(0,
                Math.min(sortedSamples.length - 1,
                        (int) Math.ceil(percentile * sortedSamples.length) - 1));
        return sortedSamples[index];
    }

    private static ChunkPos[] captureFootprint(BlockPos sample) {
        int range = FHConfig.SERVER.SIMULATION.simulationRange.get();
        int west = (Mth.floor(sample.getX() + 0.5D) - range) >> 4;
        int north = (Mth.floor(sample.getZ() + 0.5D) - range) >> 4;
        return new ChunkPos[]{
                new ChunkPos(west, north),
                new ChunkPos(west, north + 1),
                new ChunkPos(west + 1, north),
                new ChunkPos(west + 1, north + 1)
        };
    }

    private static void preload(ServerLevel level, ChunkPos[] chunks) {
        for (ChunkPos chunk : chunks) {
            level.getChunk(chunk.x, chunk.z);
        }
    }

    private static int countLoaded(ServerLevel level, ChunkPos[] chunks) {
        int loaded = 0;
        for (ChunkPos chunk : chunks) {
            if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null) {
                loaded++;
            }
        }
        return loaded;
    }

    private static void buildRoom(ServerLevel level, BlockPos center) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState();
        for (int x = -8; x <= 7; x++) {
            for (int z = -8; z <= 7; z++) {
                level.setBlock(center.offset(x, -2, z), stone, 2);
                level.setBlock(center.offset(x, 5, z), stone, 2);
            }
        }
        for (int y = -1; y <= 4; y++) {
            for (int offset = -8; offset <= 7; offset++) {
                BlockState wall = ((y + offset) & 3) == 0 ? slab : stone;
                level.setBlock(center.offset(-8, y, offset), wall, 2);
                level.setBlock(center.offset(7, y, offset), wall, 2);
                level.setBlock(center.offset(offset, y, -8), wall, 2);
                level.setBlock(center.offset(offset, y, 7), wall, 2);
            }
        }
    }

    private static void writeReport(
            ServerLevel level,
            CaptureDistribution synchronous,
            CaptureDistribution asynchronous,
            int loadedBeforeMeasurement,
            int loadedAfterMeasurement,
            int remoteLoadedBefore,
            int remoteLoadedAfter,
            int constructorTriggeredLoads,
            ResourceLocation campfireRecipe,
            BlockTempData campfire
    ) {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.addProperty("evidenceScope",
                "legacy-player-sampling-forge-gametest-capture-not-phase0b-acceptance");
        report.addProperty("generatedAt", OffsetDateTime.now().toString());
        report.addProperty("warmupIterations", WARMUP_ITERATIONS);
        report.addProperty("measurementIterations", MEASUREMENT_ITERATIONS);

        JsonObject runtime = new JsonObject();
        runtime.addProperty("serverClass", level.getServer().getClass().getName());
        runtime.addProperty("dimension", level.dimension().location().toString());
        runtime.addProperty("simulationRange",
                FHConfig.SERVER.SIMULATION.simulationRange.get());
        runtime.addProperty("simulationDivision",
                FHConfig.SERVER.SIMULATION.simulationDivision.get());
        runtime.addProperty("campfireRegistryId",
                String.valueOf(ForgeRegistries.BLOCKS.getKey(Blocks.CAMPFIRE)));
        runtime.addProperty("campfireRecipeId", campfireRecipe.toString());
        runtime.addProperty("campfireTemperature", campfire.temperature());
        runtime.addProperty("campfireMustBeLit", campfire.lit());
        report.add("forgeRuntime", runtime);

        JsonObject loadedCapture = new JsonObject();
        loadedCapture.addProperty("footprintChunks", 4);
        loadedCapture.addProperty("loadedBefore", loadedBeforeMeasurement);
        loadedCapture.addProperty("loadedAfter", loadedAfterMeasurement);
        loadedCapture.add("threadSafeFalse", synchronous.toJson());
        loadedCapture.add("threadSafeTrue", asynchronous.toJson());
        report.add("loadedFootprintCapture", loadedCapture);

        JsonObject loadProbe = new JsonObject();
        loadProbe.addProperty("loadedBefore", remoteLoadedBefore);
        loadProbe.addProperty("loadedAfter", remoteLoadedAfter);
        loadProbe.addProperty("constructorTriggeredLoads", constructorTriggeredLoads);
        loadProbe.addProperty("usesServerLevelGetChunk", true);
        report.add("unloadedFootprintProbe", loadProbe);

        JsonArray caveats = new JsonArray();
        caveats.add("Single-process Forge GameTest diagnostic; System.nanoTime samples are not JMH results.");
        caveats.add("The loaded room fixture measures full legacy construction, including four chunk lookups, heightmap reads, and optional eight-section copies.");
        caveats.add("The unloaded probe records current legacy behavior and is not a production workload.");
        caveats.add("This report does not complete Phase 0b or approve any replacement runtime.");
        report.add("caveats", caveats);

        Path output = Path.of(System.getProperty(
                        REPORT_PROPERTY,
                        "build/reports/thermal-phase0b/legacy-player-sampling-forge-capture.json"))
                .toAbsolutePath()
                .normalize();
        try {
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            Files.writeString(
                    output,
                    new GsonBuilder().setPrettyPrinting().create().toJson(report)
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("could not write Phase 0b capture report to " + output,
                    exception);
        }
    }

    private record CaptureDistribution(long p50Ns, long p95Ns, long p99Ns, long maxNs) {
        JsonObject toJson() {
            JsonObject result = new JsonObject();
            result.addProperty("p50Ns", p50Ns);
            result.addProperty("p95Ns", p95Ns);
            result.addProperty("p99Ns", p99Ns);
            result.addProperty("maxNs", maxNs);
            return result;
        }
    }

    private record CaptureDistributions(
            CaptureDistribution synchronous,
            CaptureDistribution asynchronous
    ) {
    }
}
