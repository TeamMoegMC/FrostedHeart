/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Ordinary Forge sources/geometry; the small base retains the original freshness deadline. */
@GameTestHolder("frostedheart_local_overload")
@PrefixGameTestTemplate(false)
public final class ContinuousAirOverloadProductionGameTests {
    private ContinuousAirOverloadProductionGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 2400)
    public static void denseSourceChurnDoesNotStarveSmallBase(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(true, "Use formal SHARED_GRID configuration");
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos small = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, small, Items.FLINT_AND_STEEL);
        BlockPos probe = small.above();
        BlockPos remote = helper.absolutePos(new BlockPos(517, 0, 7));
        var air = new ThermalEnvironmentSample();
        var cursor = new QueryPublication.ReadCursor();
        var overloadedAir = new QueryPublication.MutableSample();
        helper.startSequence().thenIdle(240).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe, WorldTemperature.naturalAir(level, probe), air);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(air)
                    && MinecraftThermalInput.describeAirRead(level, probe).startsWith("geometry_current"), "Warm the real small base first");
            var publication = MinecraftThermalInput.queryPublicationForObservation(level);
            int grade = -1;
            if (publication != null && publication.beginRead(cursor)) try {
                grade = cursor.airAggregationSpanAt(probe.getX() + .5, probe.getY() + .5, probe.getZ() + .5);
            } finally { cursor.clear(); }
            helper.assertTrue(grade == 1, "Warm the small base to its completed fine field before remote construction");
        }).thenExecute(() -> {
            for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 2; dz++)
                level.setChunkForced((remote.getX() >> 4) + dx, (remote.getZ() >> 4) + dz, true);
            var built = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, remote);
            // Explicit real discovery resumes through the same bounded world queue.
            var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
            for (BlockPos fire : built.fires()) chunks.add(level.getChunkAt(fire));
            for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
            long initialLimited = MinecraftThermalInput.workLimitedCompletions(level);
            long[] samples = {0}, stale = {0}, badGeometry = {0}, degraded = {0};
            long[] firstMinimumTick = {-1}, lastMinimumTick = {-1};
            int[] minimumAdvances = {0};
            boolean[] saved = {false};
            int[] maximumAge = {0}, smallGrade = {-1}, largeGrade = {-1}, maximumDiscovery = {0};
            long start = level.getGameTime();
            ContinuousAirGameTestPacing.beginMeasurement();
            FHMain.LOGGER.info("THERMAL_OVERLOAD_START,gameTick={},measuredTicks=1400,sources=129,processors={}",
                    start, Runtime.getRuntime().availableProcessors());
            ContinuousAirProductionTiming.eachTick(helper, 1400, elapsed -> {
                    if (elapsed % 400 == 0) for (BlockPos fire : built.fires())
                        level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(
                                fuel -> fuel.insertItem(0, new ItemStack(Items.COAL), false));
                    var discovery = MinecraftThermalInput.sourceDiscoveryStats(level);
                    maximumDiscovery[0] = Math.max(maximumDiscovery[0], discovery.lastTickPositions());
                    helper.assertTrue(discovery.lastTickPositions() <= 256 && discovery.activeCursors() <= 8,
                            "Real discovery must retain its position and snapshot bounds");
                    if (elapsed == 240 || elapsed == 480 || elapsed == 720 || elapsed == 960) {
                        BlockPos detail = remote.offset(24, 2, 6);
                        level.setBlockAndUpdate(detail, elapsed % 480 == 0 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
                    }
                    if (elapsed >= 300 && elapsed <= 1000 && elapsed % 40 == 0) {
                        BlockPos fire = built.fires()[(elapsed / 40) % built.fires().length];
                        ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                        level.removeBlock(fire, false);
                        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
                        level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(
                                fuel -> fuel.insertItem(0, new ItemStack(Items.COAL, 4), false));
                        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
                    }
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe, WorldTemperature.naturalAir(level, probe), air);
                    long age = air.airSampleTick() < 0 ? 9999 : level.getGameTime() - air.airSampleTick();
                    samples[0]++;
                    maximumAge[0] = Math.max(maximumAge[0], (int) age);
                    if (!air.airAvailable() || age > 40) stale[0]++;
                    if (!MinecraftThermalInput.describeAirRead(level, probe).startsWith("geometry_current")) badGeometry[0]++;
                    var publication = MinecraftThermalInput.queryPublicationForObservation(level);
                    if (publication != null && publication.beginRead(cursor)) try {
                        smallGrade[0] = cursor.airAggregationSpanAt(probe.getX() + .5, probe.getY() + .5, probe.getZ() + .5);
                        BlockPos large = built.probes()[0];
                        largeGrade[0] = cursor.airAggregationSpanAt(large.getX() + .5, large.getY() + .5, large.getZ() + .5);
                        if (largeGrade[0] >= 2 || largeGrade[0] == 0) degraded[0]++;
                        helper.assertTrue(smallGrade[0] == 1 || smallGrade[0] == -1, "The small base must retain the fine grid");
                    } finally { cursor.clear(); }
                    BlockPos large = built.probes()[0];
                    if (largeGrade[0] == 0 && publication != null && publication.tryReadAir(
                            large.getX() + .5, large.getY() + .5, large.getZ() + .5, overloadedAir)) {
                        long accepted = overloadedAir.sampleTick();
                        if (firstMinimumTick[0] < 0) firstMinimumTick[0] = accepted;
                        if (accepted > lastMinimumTick[0]) minimumAdvances[0]++;
                        lastMinimumTick[0] = Math.max(lastMinimumTick[0], accepted);
                    }
                    if (elapsed == 1100) {
                        var before = MinecraftThermalInput.observedCheckpointStatistics(level);
                        long began = System.nanoTime();
                        level.getServer().saveEverything(true, false, false);
                        var after = MinecraftThermalInput.observedCheckpointStatistics(level);
                        saved[0] = true;
                        FHMain.LOGGER.info("THERMAL_OVERLOAD_SAVE,gameTick={},fullCallUs={},thermalHandoffUs={},minimumSampleTick={},minimumAdvances={}",
                                level.getGameTime(), (System.nanoTime() - began) / 1000,
                                (after.saveWaitNanos() - before.saveWaitNanos()) / 1000, lastMinimumTick[0], minimumAdvances[0]);
                    }
                    if (elapsed % 200 == 0 || elapsed == 1400) FHMain.LOGGER.info(
                            "THERMAL_OVERLOAD_STATE,elapsed={},samples={},stale={},badGeometry={},maxAge={},smallSpan={},largeSpan={},degradedSamples={},minimumAdvances={},firstMinimumTick={},lastMinimumTick={},discoveryMax={},worker={}",
                            elapsed, samples[0], stale[0], badGeometry[0], maximumAge[0], smallGrade[0], largeGrade[0], degraded[0],
                            minimumAdvances[0], firstMinimumTick[0], lastMinimumTick[0], maximumDiscovery[0],
                            MinecraftThermalInput.describeWorkerProgress(level));
                    if (elapsed != 1400) return;
                    helper.assertTrue(level.getGameTime() - start == 1400, "Use real game ticks without synthetic thermal steps");
                    helper.assertTrue(degraded[0] > 0, "The overloaded base must publish a contracted field");
                    helper.assertTrue(minimumAdvances[0] >= 3 && lastMinimumTick[0] > firstMinimumTick[0],
                            "The bounded minimum model must actually advance physical time, not only report a coarse grid");
                    helper.assertTrue(saved[0], "Complete an actual vanilla save during the fixed overload window");
                    helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 129, "Retain every actual source");
                    helper.assertTrue(stale[0] == 0 && badGeometry[0] == 0, "Normal base isolation: stale=" + stale[0] + ",geometry=" + badGeometry[0]);
                    helper.assertTrue(MinecraftThermalInput.workLimitedCompletions(level) == initialLimited, "No repeat full-candidate budget refusals");
                    MinecraftThermalInput.closeActiveLevel(level);
                    helper.startSequence().thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                            "Actual worker close releases the shared root budget")).thenSucceed();
            });
        });
    }
}
