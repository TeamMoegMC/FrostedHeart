/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual source/geometry pressure exercises fine admission, coarse service and saves. */
@GameTestHolder("frostedheart_geometry_capacity")
@PrefixGameTestTemplate(false)
public final class ContinuousAirGeometryCapacityProductionGameTests {
    private static final int OBSERVATION_TICKS = 1600;
    @GameTest(template = "phase0a_empty", batch = "regional_capacity", timeoutTicks = 2200)
    public static void realPocketsKeepAdvancingThroughResourcePressure(GameTestHelper helper) {
        run(helper, false);
    }
    static void run(GameTestHelper helper, boolean observeIngress) {
        var level = helper.getLevel();
        BlockPos control = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, control, Items.FLINT_AND_STEEL);
        BlockPos origin = helper.absolutePos(new BlockPos(512, 0, 0));
        BlockPos[] sources = new BlockPos[1024];
        var sample = new ThermalEnvironmentSample();
        long[] firstTick = {-1}, lastTick = {-1}, maximumAge = {0};
        BlockPos[] replaced = {null};
        long[] replacementTick = {-1}, replacementSampleTick = {-1};
        var pocketSample = new QueryPublication.MutableSample();
        int[] advances = {0}, failures = {0}, missingReads = {0}, saves = {0}, unavailableRun = {0}, longestUnavailableRun = {0};
        long initialRefusals = MinecraftThermalInput.thermalBudgetRefusals();
        helper.startSequence().thenIdle(240).thenWaitUntil(() -> {
            read(helper, control.above(), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample) && span(helper, control.above()) == 1,
                    "The independent real control starts current and fine");
        }).thenExecute(() -> {
            for (int x = origin.getX() >> 4; x <= (origin.getX() + 63) >> 4; x++)
                for (int z = origin.getZ() >> 4; z <= (origin.getZ() + 63) >> 4; z++) level.setChunkForced(x, z, true);
            // Each captured 4-cubed Brick has real stone bodies and a two-block
            // source/air pocket. Nothing writes a numerical field or reduces a limit.
            for (int y = 0; y < 16; y++) for (int z = 0; z < 64; z++) for (int x = 0; x < 64; x++)
                level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
            int index = 0;
            for (int y = 0; y < 4; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockPos source = origin.offset(4 * x + 1, 4 * y + 1, 4 * z + 1);
                ignitePocket(helper, source);
                sources[index++] = source;
            }
            firstTick[0] = level.getGameTime();
            if (observeIngress) {
                com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation.arm(control.above(), sources[0].above(), sources);
                ContinuousAirSourceIngressProductionGameTests.observeUnboundDiskWrite(helper);
            }
            ContinuousAirProductionTiming.eachTick(helper, OBSERVATION_TICKS, elapsed -> {
                var publication = MinecraftThermalInput.queryPublicationForObservation(level);
                if (elapsed == 400) {
                    for (var source : sources) if (publication != null && publication.tryReadAir(source.getX() + .5,
                            source.getY() + 1.5, source.getZ() + .5, pocketSample)) {
                        replaced[0] = source;
                        level.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
                        break;
                    }
                    helper.assertTrue(replaced[0] != null, "Replace an actually admitted hot pocket under capacity pressure");
                } else if (elapsed == 421) {
                    ignitePocket(helper, replaced[0]); replacementTick[0] = level.getGameTime();
                    FHMain.LOGGER.info("THERMAL_GEOMETRY_CAPACITY_SOURCE_REPLACED,position={},tick={}", replaced[0], replacementTick[0]);
                }
                if (replacementTick[0] >= 0 && publication != null && publication.tryReadAir(replaced[0].getX() + .5,
                        replaced[0].getY() + 1.5, replaced[0].getZ() + .5, pocketSample))
                    replacementSampleTick[0] = Math.max(replacementSampleTick[0], pocketSample.sampleTick());
                read(helper, control.above(), sample);
                long tick = sample.airSampleTick(), age = tick < 0 ? -1 : level.getGameTime() - tick;
                if (tick < 0) {
                    missingReads[0]++;
                    longestUnavailableRun[0] = Math.max(longestUnavailableRun[0], ++unavailableRun[0]);
                } else { unavailableRun[0] = 0; maximumAge[0] = Math.max(maximumAge[0], age); }
                int aggregation = span(helper, control.above());
                String geometry = MinecraftThermalInput.describeAirRead(level, control.above());
                if (!ContinuousAirProductionTiming.sourceField(sample) || age < 0 || !geometry.startsWith("geometry_current")) {
                    if (failures[0]++ == 0) FHMain.LOGGER.info("THERMAL_GEOMETRY_CAPACITY_FIRST_CONTROL_FAILURE,elapsedTicks={},sampleTick={},age={},aggregation={},available={},basis={},geometry={}",
                            elapsed, tick, age, aggregation, sample.airAvailable(), sample.airBasisTerms(), geometry);
                }
                if (tick > lastTick[0]) advances[0]++;
                lastTick[0] = Math.max(lastTick[0], tick);
                if (elapsed == 600 || elapsed == 1000 || elapsed == 1400) {
                    // Use actual dirty chunks and vanilla non-flush saving while
                    // real geometry refusals and source successors are in flight.
                    for (int x = (control.getX() >> 4) - 2; x <= (control.getX() >> 4) + 2; x++)
                        for (int z = (control.getZ() >> 4) - 2; z <= (control.getZ() >> 4) + 2; z++) markDirtyLoadedChunk(helper, x, z);
                    for (int x = origin.getX() >> 4; x <= (origin.getX() + 63) >> 4; x++)
                        for (int z = origin.getZ() >> 4; z <= (origin.getZ() + 63) >> 4; z++) markDirtyLoadedChunk(helper, x, z);
                    level.getServer().saveAllChunks(true, false, false);
                    saves[0]++;
                    FHMain.LOGGER.info("THERMAL_GEOMETRY_CAPACITY_NATIVE_SAVE,elapsedTicks={},saves={}", elapsed, saves[0]);
                }
                if (elapsed % 200 == 0) FHMain.LOGGER.info("THERMAL_GEOMETRY_CAPACITY_CONTROL,elapsedTicks={},sampleTick={},maximumAge={},failures={},advances={},admissionRefusals={},worker={}",
                        elapsed, tick, maximumAge[0], failures[0], advances[0], MinecraftThermalInput.thermalBudgetRefusals() - initialRefusals,
                        MinecraftThermalInput.describeWorkerProgress(level));
            });
        }).thenIdle(OBSERVATION_TICKS + 1).thenExecute(() -> {
            if (observeIngress) FHMain.LOGGER.info("THERMAL_SOURCE_INGRESS_CAPACITY,{}",
                    com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation.snapshot());
            helper.assertTrue(MinecraftThermalInput.thermalBudgetRefusals() > initialRefusals,
                    "Actual fine-work admission must refuse space during resource pressure");
            helper.assertTrue(replacementTick[0] >= 0 && replacementSampleTick[0] >= replacementTick[0] + 600,
                    "The admitted pocket keeps advancing after an actual source identity replacement under refusal: replacementTick="
                            + replacementTick[0] + ",sampleTick=" + replacementSampleTick[0] + ",geometry="
                            + (replaced[0] == null ? "not_replaced" : MinecraftThermalInput.describeAirRead(level, replaced[0].above())));
            helper.assertTrue(saves[0] == 3, "Complete all three real vanilla saves during ongoing capacity refusal");
            helper.assertTrue(advances[0] >= 20 && lastTick[0] >= firstTick[0] + OBSERVATION_TICKS - 100,
                    "Refusal and retry must preserve readable, advancing independent service: age=" + maximumAge[0]
                            + ",missingReads=" + missingReads[0] + ",failures=" + failures[0]);
            FHMain.LOGGER.info("THERMAL_GEOMETRY_CAPACITY_PRODUCTION,sources={},advances={},maximumAge={},failures={},saves={},admissionRefusals={},rootBytes={},unavailableTicks={},longestUnavailableRun={}",
                    sources.length, advances[0], maximumAge[0], failures[0], saves[0], MinecraftThermalInput.thermalBudgetRefusals() - initialRefusals,
                    MinecraftThermalInput.thermalBudgetUsedBytes(), missingReads[0], longestUnavailableRun[0]);
            if (observeIngress) {
                var observed = com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation.snapshot();
                helper.assertTrue(observed.unboundSnapshots() > 0 && observed.unboundDiskWrites() > 0 && observed.encodingFailures() == 0,
                        "A real geometry wait must retain accepted source J through actual serialization and disk flush: " + observed);
                helper.assertTrue(observed.orderingFailures() == 0 && observed.maximumOwnedWindows() <= 2 && observed.maximumImbalanceJ() < 1e-6,
                        "Retained source windows remain bounded, ordered and owned once under actual pressure: " + observed);
                com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation.stop();
            }
            for (var source : sources) level.setBlockAndUpdate(source,
                    level.getBlockState(source).setValue(CampfireBlock.LIT, false));
            ContinuousAirProductionGameTests.useItem(helper, control, Items.IRON_SHOVEL);
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                "Closing releases retained inputs, actual helpers and notifications"))
        .thenExecute(() -> {
            for (int x = origin.getX() >> 4; x <= (origin.getX() + 63) >> 4; x++)
                for (int z = origin.getZ() >> 4; z <= (origin.getZ() + 63) >> 4; z++) level.setChunkForced(x, z, false);
        }).thenSucceed();
    }

    private static void markDirtyLoadedChunk(GameTestHelper helper, int x, int z) {
        var chunk = helper.getLevel().getChunkSource().getChunkNow(x, z);
        if (chunk != null) chunk.setUnsaved(true);
    }

    private static void ignitePocket(GameTestHelper helper, BlockPos source) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(source.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        level.getBlockEntity(source).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new IllegalStateException("Actual campfire fuel capability is missing"))
                .insertItem(0, new ItemStack(Items.COAL, 4), false);
        ContinuousAirProductionGameTests.useItem(helper, source, Items.FLINT_AND_STEEL);
    }

    private static void read(GameTestHelper helper, BlockPos point, ThermalEnvironmentSample sample) {
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(helper.getLevel(), point,
                WorldTemperature.naturalAir(helper.getLevel(), point), sample);
    }
    private static int span(GameTestHelper helper, BlockPos point) {
        var publication = MinecraftThermalInput.queryPublicationForObservation(helper.getLevel());
        var cursor = new QueryPublication.ReadCursor();
        try {
            if (publication == null || !publication.beginRead(cursor)) return -1;
            return cursor.airAggregationSpanAt(point.getX() + .5, point.getY() + .5, point.getZ() + .5);
        } finally { cursor.clear(); }
    }
}
