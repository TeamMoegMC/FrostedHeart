/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalBoundaryObservation;
import com.teammoeg.frostedheart.content.climate.thermal.solver.StructuralIsolationObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Cold construction happens after the independent room has a real committed field. */
@GameTestHolder("frostedheart_independent_intake")
@PrefixGameTestTemplate(false)
public final class ContinuousAirIndependentIntakeProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 2200)
    public static void coldDenseBasesDoNotHoldFourIndependentInputWindows(GameTestHelper helper) throws java.io.IOException {
        run(helper, false);
    }

    static void run(GameTestHelper helper, boolean structural) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        BlockPos remote = helper.absolutePos(new BlockPos(517, 0, 7));
        BlockPos[] origins = {remote, remote.east(512)};
        var air = new ThermalEnvironmentSample();
        long[] started = {-1}, maximumAge = {0};
        int[] samples = {0}, unavailable = {0}, stale = {0}, constructionChanges = {0};
        var bases = new ContinuousAirLargeBaseProductionGameTests.BuiltBase[2];
        BlockPos newOrigin = helper.absolutePos(new BlockPos(128, 0, 0));
        BlockPos[] newFire = {null};
        long[] newStarted = {-1}, newReady = {-1};
        var newAir = new ThermalEnvironmentSample();
        if (structural) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            level.setChunkForced((newOrigin.getX() >> 4) + dx, (newOrigin.getZ() >> 4) + dz, true);

        helper.startSequence().thenIdle(200).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), air);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(air)
                    && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "The independent room must be warm and current before constructing either dense base");
        }).thenExecute(() -> {
            RegionalBoundaryObservation.requirePendingRoutes(false);
            RegionalBoundaryObservation.requireConsecutiveWindows(4);
            RegionalBoundaryObservation.arm(RegionalBoundaryObservation.Mode.REMOTE_GEOMETRY, fire.above(), remote.offset(10, 3, 7));
            if (structural) StructuralIsolationObservation.arm(newOrigin.offset(8, 1, 8), remote);
            for (int index = 0; index < origins.length; index++) {
                var origin = origins[index];
                for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
                    level.setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, true);
                bases[index] = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origin);
                var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
                for (var source : bases[index].fires()) chunks.add(level.getChunkAt(source));
                for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
            }
            started[0] = level.getGameTime();
            FHMain.LOGGER.info("THERMAL_INDEPENDENT_INTAKE_START,gameTick={},sources=257,requiredWindows=4", started[0]);
            for (int delay = 1; delay <= 800; delay++) {
                int elapsed = delay;
                helper.runAfterDelay(delay, () -> {
                    // Continue actual remote construction across several source windows.
                    // A single cold build can finish before four later cuts exist.
                    if (elapsed <= 200 && elapsed % 5 == 0) {
                        int wave = elapsed / 5 - 1;
                        int first = (wave % 8) * 16;
                        var state = (wave / 8 % 2 == 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState();
                        for (var base : bases) for (int source = first; source < first + 16; source++)
                            for (int dy = 1; dy <= 4; dy++)
                                if (level.setBlockAndUpdate(base.fires()[source].east().above(dy), state)) constructionChanges[0]++;
                    }
                    if (structural && StructuralIsolationObservation.takeWorldAction()) {
                        newStarted[0] = level.getGameTime();
                        newFire[0] = ContinuousAirProductionGameTests.room(helper, newOrigin);
                        ContinuousAirProductionGameTests.useItem(helper, newFire[0], Items.FLINT_AND_STEEL);
                        MinecraftThermalInput.onChunkLoad(level, level.getChunkAt(newFire[0]));
                        FHMain.LOGGER.info("THERMAL_STRUCTURE_NEW_ROOM,gameTick={}", newStarted[0]);
                    }
                    if (newFire[0] != null && newReady[0] < 0) {
                        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, newFire[0].above(),
                                WorldTemperature.naturalAir(level, newFire[0].above()), newAir);
                        if (ContinuousAirProductionTiming.sourceField(newAir)
                                && MinecraftThermalInput.describeAirRead(level, newFire[0].above()).startsWith("geometry_current")) {
                            newReady[0] = level.getGameTime();
                            FHMain.LOGGER.info("THERMAL_STRUCTURE_NEW_ROOM_READY,gameTick={},elapsed={},{}",
                                    newReady[0], newReady[0] - newStarted[0], StructuralIsolationObservation.summary());
                        }
                    }
                    if (RegionalBoundaryObservation.takeWorldAction())
                        level.setBlockAndUpdate(remote.offset(10, 3, 7), Blocks.STONE.defaultBlockState());
                    if (elapsed == 21 || elapsed == 61) {
                        var source = bases[0].fires()[0];
                        boolean lit = level.getBlockState(source).getValue(CampfireBlock.LIT);
                        if (elapsed == 21 && lit) ContinuousAirProductionGameTests.useItem(helper, source, Items.IRON_SHOVEL);
                        if (elapsed == 61 && !lit) ContinuousAirProductionGameTests.useItem(helper, source, Items.FLINT_AND_STEEL);
                    }
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), air);
                    samples[0]++;
                    if (!ContinuousAirProductionTiming.sourceField(air)) {
                        unavailable[0]++;
                        FHMain.LOGGER.info("THERMAL_INDEPENDENT_INTAKE_GAP,elapsed={},gameTick={},sampleTick={},query={}",
                                elapsed, level.getGameTime(), air.airSampleTick(), MinecraftThermalInput.describeAirRead(level, fire.above()));
                    }
                    else maximumAge[0] = Math.max(maximumAge[0], level.getGameTime() - air.airSampleTick());
                    if (!MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current")) stale[0]++;
                    if (elapsed % 100 == 0) FHMain.LOGGER.info("THERMAL_INDEPENDENT_INTAKE_WINDOW,elapsed={},maxAge={},unavailable={},stale={},{}",
                            elapsed, maximumAge[0], unavailable[0], stale[0], RegionalBoundaryObservation.summary());
                });
            }
        }).thenIdle(820).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_INDEPENDENT_INTAKE_RESULT,samples={},maxAge={},unavailable={},stale={},constructionChanges={},{}",
                    samples[0], maximumAge[0], unavailable[0], stale[0], constructionChanges[0], RegionalBoundaryObservation.summary());
            helper.assertTrue(RegionalBoundaryObservation.failure() == null, RegionalBoundaryObservation.summary());
            helper.assertTrue(RegionalBoundaryObservation.completed(),
                    "Observe an actual independent commit and four consecutive complete source windows");
            helper.assertTrue(maximumAge[0] <= 40 && unavailable[0] == 0 && stale[0] == 0,
                    "The independent room must remain current and within the original 40-tick limit");
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == (structural ? 258 : 257), "Every actual source must remain accounted");
            if (structural) {
                helper.assertTrue(StructuralIsolationObservation.crossed(), "No local structure committed during an earlier remote task: " + StructuralIsolationObservation.summary());
                helper.assertTrue(newReady[0] >= 0 && newReady[0] - newStarted[0] <= 40,
                        "The additional real room must become current within 40 ticks; elapsed=" + (newReady[0] - newStarted[0]));
                StructuralIsolationObservation.end();
            }
            RegionalBoundaryObservation.end();
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0, "All real runtime owners must retire on close"))
        .thenExecute(() -> {
            if (structural) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                level.setChunkForced((newOrigin.getX() >> 4) + dx, (newOrigin.getZ() >> 4) + dz, false);
            for (var origin : origins) for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 3; dz++)
                level.setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, false);
        }).thenSucceed();
    }
}
