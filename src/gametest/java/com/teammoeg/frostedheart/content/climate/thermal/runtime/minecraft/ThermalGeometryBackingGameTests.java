/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_geometry_backing")
@PrefixGameTestTemplate(false)
public final class ThermalGeometryBackingGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_backing", timeoutTicks = 600)
    public static void repeatedLocalEditsStayChargedWithoutRepeatedDimensionInventories(GameTestHelper helper) throws Exception {
        var level = helper.getLevel(); var origin = new BlockPos(565248, 192, 565248); var wall = origin.offset(1, 1, 1);
        var expansion = origin.east(64);
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        level.setChunkForced(expansion.getX() >> 4, expansion.getZ() >> 4, true);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            for (var base : new BlockPos[] {origin, expansion}) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false)) {
            var inventory = simulation.engine.getClass().getDeclaredMethod("airBackingBytes"); inventory.setAccessible(true);
            var completion = simulation.engine.getClass().getDeclaredMethod("completionCapacityBytes"); completion.setAccessible(true);
            long initialRuns = runs(simulation), probeRuns = 0, maximumConservativeBytes = 0;
            for (int edit = 0; edit < 6; edit++) {
                var next = (edit % 2 == 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState();
                simulation.publish(simulation.change(wall, next, ResolvedGeometryBatch.MaterialChanges.REPLACE), ThermalInputBatch.NO_PHASE_ACKS);
                long required = (long) inventory.invoke(simulation.engine) + (long) completion.invoke(simulation.engine); probeRuns++;
                ThermalMemoryBudget.Reservation retained = ThermalLoadedWorldGameTests.read(simulation.engine, "airReservation");
                helper.assertTrue(retained.bytes() >= required, "Every installed value, previous publication and shared witness stays charged before reconciliation; required="
                        + required + ",charged=" + retained.bytes());
                maximumConservativeBytes = Math.max(maximumConservativeBytes, retained.bytes() - required);
                helper.assertTrue(simulation.fixed.acceptedTick() == 0, "Geometry accounting does not introduce a physical catch-up step");
            }
            long editInventories = runs(simulation) - initialRuns - probeRuns;
            var page = new com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle(net.minecraft.core.SectionPos.asLong(expansion), 1);
            byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                    simulation.capture.captureBricks(page.sectionKey(), null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            long beforePressureRuns = runs(simulation);
            try (var occupied = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(occupied != null, "Apply real optional-admission pressure while small handoffs await reconciliation");
                simulation.engine.offerInput(new com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope(
                        new ThermalInputBatch(1, ++simulation.sequence, 0, new ThermalInputBatch.PageAdmission[] {admission},
                                ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                                ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN),
                        new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Window[0]));
                for (int round = 0; round < 256 && simulation.engine.hasReadyWork(); round++) {
                    simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
                }
                helper.assertTrue(!simulation.engine.hasReadyWork() && !simulation.fixed.hasSection(page.sectionKey()),
                        "After one pressure reconciliation the genuinely full admission waits without repeatedly cancelling/restarting its candidate");
                helper.assertTrue(runs(simulation) - beforePressureRuns == 1, "Capacity pressure reconciles the pending handoffs exactly once");
            }
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(simulation.fixed.hasSection(page.sectionKey()), "A real capacity release resumes the same spatial admission");
            long beforeIntervalRuns = runs(simulation);
            simulation.tick = 20; simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            for (int round = 0; round < 256 && simulation.engine.hasReadyWork(); round++) {
                simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
            }
            long afterIntervalRuns = runs(simulation) - initialRuns - probeRuns;
            long required = (long) inventory.invoke(simulation.engine) + (long) completion.invoke(simulation.engine);
            ThermalMemoryBudget.Reservation retained = ThermalLoadedWorldGameTests.read(simulation.engine, "airReservation");
            helper.assertTrue(retained.bytes() >= required, "Interval reconciliation preserves the complete retained union");
            FHMain.LOGGER.info("THERMAL_GEOMETRY_BACKING,localEdits=6,editInventories={},afterIntervalInventories={},maximumConservativeBytes={},chargedBytes={},unionBytes={},acceptedTick={},pressureInventory=1,waitingAdmissionResumed=true",
                    editInventories, afterIntervalRuns, maximumConservativeBytes, retained.bytes(), required, simulation.fixed.acceptedTick());
            helper.assertTrue(editInventories <= 1, "Six small edits must not rescan the whole dimension six times; actual=" + editInventories);
            helper.assertTrue(runs(simulation) - beforeIntervalRuns - 1 == 1,
                    "The next physical interval reconciles accumulated ownership once");
        } finally {
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
            level.setChunkForced(expansion.getX() >> 4, expansion.getZ() >> 4, false);
        }
        helper.succeed();
    }

    private static long runs(ThermalMaterialBehaviorGameTests.Simulation simulation) {
        return ThermalLoadedWorldGameTests.<Long>read(simulation.engine, "backingInventoryRuns");
    }
}
