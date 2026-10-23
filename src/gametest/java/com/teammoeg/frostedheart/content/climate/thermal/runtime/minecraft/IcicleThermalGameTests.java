/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.block.IcicleBlock;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_icicle")
@PrefixGameTestTemplate(false)
public final class IcicleThermalGameTests {
    @GameTest(template = "phase0a_empty", batch = "icicle_worker_cycle", timeoutTicks = 80)
    public static void phaseReceiptConsumesBudgetThroughRealWorkerMigration(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var anchor = helper.absolutePos(new BlockPos(4, 12, 4));
        var origin = new BlockPos(anchor.getX() & ~3, anchor.getY() & ~3, anchor.getZ() & ~3);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
        var source = origin.offset(1, 2, 1);
        level.setBlockAndUpdate(source, FHBlocks.ICICLE.get().defaultBlockState());
        level.setBlockAndUpdate(source.east(), Blocks.AIR.defaultBlockState());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 2)) {
            int slot = simulation.slot(source);
            var edge = simulation.arena.materialLaw(slot).heating();
            double budget = edge.targetEnthalpyJ() - edge.sourceEnthalpyJ();
            near(helper, budget, simulation.arena.acceptExternalEnergyJ(slot, budget * 2), "heat stops at one latent endpoint");
            var ready = simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(ready.phaseRequests().length == 1, "one completed material request");
            var request = ready.phaseRequests()[0];
            double ledger = simulation.arena.externalMaterialEnergyJ();
            var receipt = level.getBlockState(source).cycle(IcicleBlock.MELT_CYCLE);
            var geometry = simulation.change(source, receipt, ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION);
            simulation.publish(geometry, new ThermalInputBatch.PhaseAck[]{
                    new ThermalInputBatch.PhaseAck(request, PhaseTransitionRuntime.AckOutcome.APPLIED)});
            slot = simulation.slot(source);
            near(helper, edge.sourceEnthalpyJ(), simulation.arena.enthalpyJ(slot), "existing material migration consumes the paid latent interval");
            near(helper, -budget, simulation.arena.externalMaterialEnergyJ() - ledger, "phase work exported through existing energy ledger");
            simulation.publish(ResolvedGeometryBatch.EMPTY, new ThermalInputBatch.PhaseAck[]{
                    new ThermalInputBatch.PhaseAck(request, PhaseTransitionRuntime.AckOutcome.APPLIED)});
            near(helper, edge.sourceEnthalpyJ(), simulation.arena.enthalpyJ(slot), "duplicate old ACK cannot pay twice");
            var saved = MaterialSectionState.capture(simulation.page(source).currentPublication(), simulation.query,
                    simulation.profiles.signatures(), new MaterialSectionState.CaptureScratch());
            helper.assertTrue(saved.valid(), "completed cycle produces a coherent material checkpoint");
            int index = saved.state().find((source.getX() & 15) | (source.getZ() & 15) << 4 | (source.getY() & 15) << 8);
            near(helper, edge.sourceEnthalpyJ(), saved.state().enthalpyJ(index), "checkpoint contains consumed progress");
            near(helper, budget / 2, simulation.arena.acceptExternalEnergyJ(slot, budget / 2), "source can absorb another interval");
            var next = simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(next.phaseRequests().length == 0, "half another budget cannot trigger another shortening");
        }
        helper.succeed();
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-6 * Math.max(1, Math.abs(expected)), message + ": " + actual);
    }
}
