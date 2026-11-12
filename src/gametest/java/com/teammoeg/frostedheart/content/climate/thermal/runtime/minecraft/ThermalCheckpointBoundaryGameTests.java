/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.CheckpointBoundaryProbe;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAirRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_checkpoint_boundary")
@PrefixGameTestTemplate(false)
public final class ThermalCheckpointBoundaryGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_checkpoint_boundary", timeoutTicks = 200)
    public static void unloadJobCleanupAnnouncesNewGeometryBeforeNumericalWork(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var anchor = helper.absolutePos(new BlockPos(8, 8, 8));
        var origin = new BlockPos(anchor.getX() & ~3, anchor.getY() & ~3, anchor.getZ() & ~3);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z),
                    (x == 0 || y == 0 || z == 0 || x == 3 || y == 3 || z == 3 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16);
                var capture = producer.capture(40)) {
            RegionalAirRuntime runtime = ThermalLoadedWorldGameTests.read(simulation.engine, "regional");
            runtime.sealSpatialInput(40, Double.NaN);
            helper.assertTrue(CheckpointBoundaryProbe.jobs(runtime) > 0, "Unload must cancel actual queued numerical jobs");
            var checkpoint = simulation.engine.requestCheckpoint(new ChunkPos(origin).toLong(), capture,
                    PendingHeatCheckpoint.EMPTY, 1200, RegionalCheckpoint.Purpose.UNLOAD);
            var geometry = simulation.change(origin.offset(1, 1, 1), Blocks.IRON_BLOCK.defaultBlockState(),
                    ResolvedGeometryBatch.MaterialChanges.REPLACE);
            var input = new ThermalInputBatch(1, ++simulation.sequence, 40, ThermalInputBatch.NO_ADMISSIONS,
                    ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, geometry,
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
            simulation.engine.offerInput(new ThermalInputEnvelope(input, new SpatialSourceTimeline.Window[0]));
            simulation.engine.processWorkRound();
            helper.assertTrue(!checkpoint.isDone(), "Observe the actual unload-cleanup round before checkpoint capture");
            helper.assertTrue(CheckpointBoundaryProbe.allHeldThrough(runtime, 40),
                    "Numerical cleanup must announce incoming geometry even while a checkpoint has priority");
            var cancelled = simulation.engine.cancelCheckpoint(checkpoint);
            for (int round = 0; round < 100 && !cancelled.isDone(); round++) simulation.engine.processWorkRound();
            helper.assertTrue(cancelled.isDone(), "Return the real checkpoint ownership before closing the fixture");
            simulation.tick = 40;
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            FHMain.LOGGER.info("THERMAL_CHECKPOINT_BOUNDARY_RESULT,actualQueuedJobs=true,unloadCleanup=true,newGeometryHeld=true");
        }
        helper.succeed();
    }
}
