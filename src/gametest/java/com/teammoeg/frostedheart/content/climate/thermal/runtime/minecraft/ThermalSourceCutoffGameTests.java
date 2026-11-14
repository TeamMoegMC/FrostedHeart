/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Uses actual producer window ownership and the original fixed-engine intake. */
@GameTestHolder("frostedheart_source_cutoff")
@PrefixGameTestTemplate(false)
public final class ThermalSourceCutoffGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_source_cutoff", timeoutTicks = 600)
    public static void settledSourcesSkipGeometryButUnannouncedAndUnsealedControlsStillStopTheClock(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(360448, 192, 360448); var fire = origin.offset(1, 0, 1);
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var profile = simulation.profiles.tuning().campfire();
            helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                            fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                            20, true, 0), "Register a real source with the ordinary physical outlet resolver");
            submit(helper, simulation, 0, producer.seal(0));
            submit(helper, simulation, 20, producer.seal(20));
            SpatialSourceIngress ingress = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceIngress");
            helper.assertTrue(ingress.firstPendingTick(section -> true) == Long.MAX_VALUE, "The producer prefix is fully handed off");
            int[] sectionChecks = {0};
            java.util.function.LongPredicate captured = section -> { sectionChecks[0]++; return true; };
            for (int query = 0; query < 20000; query++)
                helper.assertTrue(ingress.firstPendingTick(captured) == Long.MAX_VALUE, "A settled source has no earlier cutoff");

            producer.power(fire.asLong(), 40, 40); var first = producer.seal(40);
            producer.power(fire.asLong(), 80, 60); var second = producer.seal(60);
            producer.enabled(fire.asLong(), false, 80);
            helper.assertTrue(first.length == 1 && second.length == 1 && producer.seal(80).length == 0,
                    "Hold both actual transport windows so a third control remains unsealed");
            helper.assertTrue(ingress.firstPendingTick(section -> true) == 40,
                    "A sealed but not yet enqueued window already exposes its physical control boundary");
            helper.assertTrue(ingress.firstPendingTick(section -> false) == Long.MAX_VALUE,
                    "An unavailable Section does not block unrelated captured Sections");
            submit(helper, simulation, 40, first);
            helper.assertTrue(ingress.firstPendingTick(section -> true) == 60, "The next unannounced control still limits the clock");
            submit(helper, simulation, 60, second);
            helper.assertTrue(ingress.firstPendingTick(section -> true) == 80,
                    "The unsealed control remains visible after both older windows have been consumed");
            var last = producer.seal(80);
            helper.assertTrue(last.length == 1, "Freed transport capacity captures the held control");
            submit(helper, simulation, 80, last);
            helper.assertTrue(ingress.firstPendingTick(section -> true) == Long.MAX_VALUE && simulation.fixed.acceptedTick() == 80,
                    "The fixed engine reaches the original clock after each control is delivered once");
            FHMain.LOGGER.info("THERMAL_SOURCE_CUTOFF,settledQueries=20000,sectionChecks={},sealedCutoffs=40:60,unsealedCutoff=80,finalTick={}",
                    sectionChecks[0], simulation.fixed.acceptedTick());
            helper.assertTrue(sectionChecks[0] == 0, "Settled source cutoffs must not repeatedly resolve captured geometry");
        } finally {
            MinecraftThermalInput.closeActiveLevel(level);
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
        }
        helper.succeed();
    }

    private static void submit(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, long tick,
            SpatialSourceTimeline.Window[] windows) {
        var input = new ThermalInputBatch(1, ++simulation.sequence, tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
        var result = RegionalEngineTestDriver.submit(helper, simulation.engine, new ThermalInputEnvelope(input, windows));
        helper.assertTrue(result.status() == com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion.Status.COMPLETED,
                "Deliver real source windows through the original engine");
    }
}
