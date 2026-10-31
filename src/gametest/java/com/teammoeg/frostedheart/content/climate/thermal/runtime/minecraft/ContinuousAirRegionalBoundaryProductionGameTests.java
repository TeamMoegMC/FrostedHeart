/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalBoundaryObservation;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalBoundaryObservation.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_regional_boundary")
@PrefixGameTestTemplate(false)
public final class ContinuousAirRegionalBoundaryProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 3000)
    public static void distantGeometryAndRealOutletChangesKeepTheirPhysicalBoundaries(GameTestHelper helper) throws java.io.IOException {
        run(helper, false);
    }

    static void run(GameTestHelper helper, boolean pendingRoutesRequired) throws java.io.IOException {
        run(helper, pendingRoutesRequired, false);
    }

    static void runDependencies(GameTestHelper helper) throws java.io.IOException { run(helper, false, true); }

    private static void run(GameTestHelper helper, boolean pendingRoutesRequired, boolean onlyDependencies) throws java.io.IOException {
        RegionalBoundaryObservation.requirePendingRoutes(pendingRoutesRequired);
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        BlockPos remote = helper.absolutePos(new BlockPos(517, 0, 7));
        BlockPos[] remoteOrigins = {remote, remote.east(512)};
        for (var origin : remoteOrigins) for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
            level.setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, true);
        var bases = new ContinuousAirLargeBaseProductionGameTests.BuiltBase[] {
                ContinuousAirLargeBaseProductionGameTests.buildBase(helper, remoteOrigins[0]),
                ContinuousAirLargeBaseProductionGameTests.buildBase(helper, remoteOrigins[1])};
        var changingBases = pendingRoutesRequired ? bases : new ContinuousAirLargeBaseProductionGameTests.BuiltBase[] {bases[0]};
        // A taller/deeper actual hall increases Air work without adding source
        // identities or deliberately exhausting the routing visit allowance.
        for (int y = 0; y <= 24; y++) for (int z = 0; z < 26; z++) for (int x = 0; x < 50; x++) {
            if (y < 12 && z < 13) continue;
            boolean boundary = y == 0 || y == 6 || y == 24 || x == 0 || x == 49 || z == 0 || z == 25;
            boolean partition = x == 12 || x == 24 || x == 36;
            boolean passage = z == 6 && y % 6 >= 1 && y % 6 <= 2;
            level.setBlockAndUpdate(remote.offset(x, y, z), boundary || partition && !passage
                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
        for (var base : bases) for (var source : base.fires()) chunks.add(level.getChunkAt(source));
        for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
        BlockPos minor = remote.offset(10, 3, 7), outlet = bases[0].fires()[0].above(), local = fire.east(2).above(2);
        var sample = new ThermalEnvironmentSample();
        long[] maximumAge = {0}; int[] geometryFailures = {0}, unavailableLocal = {0}, localSamples = {0};
        int[] remoteAttempts = {0}, pillarDirection = {-1};
        boolean[] remotePassed = {false};
        for (int delay = 1; delay < 2950; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                // Real scheduler overlap varies between launches. Try at most three
                // actual building alterations, from drained input each time. A short
                // build is not evidence that two later cuts crossed its boundary.
                if (elapsed % 200 == 0 && remoteAttempts[0] > 0 && remoteAttempts[0] < 3
                        && RegionalBoundaryObservation.mode() == Mode.REMOTE_GEOMETRY
                        && !RegionalBoundaryObservation.completed() && RegionalBoundaryObservation.failure() == null
                        && RegionalBoundaryObservation.quiet()) {
                    level.setBlockAndUpdate(minor, Blocks.AIR.defaultBlockState());
                    RegionalBoundaryObservation.arm(Mode.REMOTE_GEOMETRY, fire.above(), minor);
                    remoteAttempts[0]++;
                    pillarDirection[0] = (pillarDirection[0] + 1) % 3;
                    pillars(helper, changingBases, pillarDirection[0]);
                    ceilingBeams(helper, changingBases, remoteAttempts[0] == 2);
                    FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_RETRY,elapsed={},attempt={},pillarDirection={},realCeilingBeams={}",
                            elapsed, remoteAttempts[0], pillarDirection[0], remoteAttempts[0] == 2);
                }
                if (RegionalBoundaryObservation.takeWorldAction()) {
                    BlockPos changed = switch (RegionalBoundaryObservation.mode()) {
                        case REMOTE_GEOMETRY -> minor;
                        case SOURCE_OUTLET -> outlet;
                        case LOCAL_GEOMETRY -> local;
                    };
                    helper.assertTrue(level.getBlockState(changed).isAir(), "The actual later world edit must replace Air");
                    level.setBlockAndUpdate(changed, Blocks.STONE.defaultBlockState());
                    FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_ACTION,mode={},elapsed={},position={}",
                            RegionalBoundaryObservation.mode(), elapsed, changed);
                }
                if (RegionalBoundaryObservation.mode() == Mode.REMOTE_GEOMETRY) {
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), sample);
                    if (!ContinuousAirProductionTiming.sourceField(sample)) {
                        unavailableLocal[0]++;
                        if (unavailableLocal[0] <= 3) FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_UNAVAILABLE,elapsed={},worldTick={},sampleTick={},available={},basis={},read={},worker={}",
                                elapsed, level.getGameTime(), sample.airSampleTick(), sample.airAvailable(), sample.airBasisTerms(),
                                MinecraftThermalInput.describeAirRead(level, fire.above()), MinecraftThermalInput.describeWorkerProgress(level));
                    }
                    else {
                        localSamples[0]++;
                        maximumAge[0] = Math.max(maximumAge[0], level.getGameTime() - sample.airSampleTick());
                    }
                    if (!MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current")) geometryFailures[0]++;
                }
                if (elapsed % 200 == 0) FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_PROGRESS,elapsed={},{}", elapsed, RegionalBoundaryObservation.summary());
                if (elapsed % 300 == 0) for (var base : bases) for (var source : base.fires())
                    level.getBlockEntity(source).getCapability(ForgeCapabilities.ITEM_HANDLER)
                            .orElseThrow(() -> new IllegalStateException("Actual fuel capability disappeared"))
                            .insertItem(0, new ItemStack(Items.COAL), false);
            });
        }
        Runnable assertPhase = () -> {
            helper.assertTrue(RegionalBoundaryObservation.failure() == null, RegionalBoundaryObservation.summary());
            helper.assertTrue(RegionalBoundaryObservation.completed(), "Actual later cut or retained boundary was not observed: " + RegionalBoundaryObservation.summary());
            helper.assertTrue(RegionalBoundaryObservation.quiet(), "The real pending cuts must finish before the next world operation");
        };
        var sequence = helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 257, "Use one probe room and two actual 128-source bases");
            for (BlockPos probe : new BlockPos[] {fire.above(), bases[0].probes()[0], bases[1].probes()[0], remote.offset(10, 15, 6)}) {
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe, WorldTemperature.naturalAir(level, probe), sample);
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                                && MinecraftThermalInput.describeAirRead(level, probe).startsWith("geometry_current"),
                        "All real fine fields must exist before changing the remote halls");
            }
            helper.assertTrue(RegionalBoundaryObservation.quiet(), "Begin from actually drained input");
        });
        if (!onlyDependencies) {
        sequence.thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_SCENE,enabledSources={},remoteBases={},extendedHall=true,pendingRoutesRequired={}",
                    MinecraftThermalInput.enabledPhysicalSourceCount(level), bases.length, pendingRoutesRequired);
            RegionalBoundaryObservation.arm(Mode.REMOTE_GEOMETRY, fire.above(), minor);
            remoteAttempts[0] = 1; pillarDirection[0] = 0;
            pillars(helper, changingBases, pillarDirection[0]);
        }).thenWaitUntil(() -> {
            helper.assertTrue(RegionalBoundaryObservation.failure() == null, RegionalBoundaryObservation.summary());
            helper.assertTrue(RegionalBoundaryObservation.completed()
                            || remoteAttempts[0] == 3 && RegionalBoundaryObservation.actionIssued() && RegionalBoundaryObservation.quiet(),
                    "Wait for observed crossing or the three completed real build attempts");
            helper.assertTrue(RegionalBoundaryObservation.quiet(), "Drain real input before the independent negative cases");
        }).thenExecute(() -> {
            remotePassed[0] = RegionalBoundaryObservation.completed();
            FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_PRODUCTION,{}", RegionalBoundaryObservation.summary());
            FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_FRESHNESS,localSamples={},maxLocalAgeTicks={},unavailableLocal={},geometryFailures={}",
                    localSamples[0], maximumAge[0], unavailableLocal[0], geometryFailures[0]);
            if (pendingRoutesRequired) helper.assertTrue(maximumAge[0] <= 40 && unavailableLocal[0] == 0 && geometryFailures[0] == 0,
                    "Unfinished distant routes must preserve the independent room's original freshness and geometry");
        });
        }
        sequence.thenExecute(() -> {
            RegionalBoundaryObservation.arm(Mode.SOURCE_OUTLET, bases[0].probes()[0], outlet);
            pillarDirection[0] = (pillarDirection[0] + 1) % 3;
            pillars(helper, changingBases, pillarDirection[0]);
        }).thenWaitUntil(assertPhase).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_PRODUCTION,{}", RegionalBoundaryObservation.summary());
            RegionalBoundaryObservation.end();
            level.setBlockAndUpdate(outlet, Blocks.AIR.defaultBlockState());
        }).thenIdle(80).thenWaitUntil(() -> helper.assertTrue(RegionalBoundaryObservation.quiet(), "The actual outlet restoration must finish"))
        .thenExecute(() -> {
            RegionalBoundaryObservation.arm(Mode.LOCAL_GEOMETRY, fire.above(), local);
            pillarDirection[0] = (pillarDirection[0] + 1) % 3;
            pillars(helper, changingBases, pillarDirection[0]);
        }).thenWaitUntil(assertPhase).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_REGIONAL_BOUNDARY_PRODUCTION,{}", RegionalBoundaryObservation.summary());
            RegionalBoundaryObservation.end();
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0, "Close must release all boundaries and source receipts"))
        .thenExecute(() -> {
            for (var origin : remoteOrigins) for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
                level.setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, false);
            if (!onlyDependencies) helper.assertTrue(remotePassed[0], "The required real two-cut structural crossing was not covered in three actual builds");
        }).thenSucceed();
    }

    private static void pillars(GameTestHelper helper, ContinuousAirLargeBaseProductionGameTests.BuiltBase[] bases, int direction) {
        for (var base : bases) for (var fire : base.fires()) for (int y = 1; y <= 4; y++) {
            helper.getLevel().setBlockAndUpdate(fire.west().above(y), direction == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(fire.east().above(y), direction == 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(fire.north().above(y), direction == 2 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    private static void ceilingBeams(GameTestHelper helper, ContinuousAirLargeBaseProductionGameTests.BuiltBase[] bases, boolean present) {
        for (var base : bases) for (var fire : base.fires()) for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) for (int y = 3; y <= 4; y++)
                helper.getLevel().setBlockAndUpdate(fire.offset(x, y, z), present
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }
}
