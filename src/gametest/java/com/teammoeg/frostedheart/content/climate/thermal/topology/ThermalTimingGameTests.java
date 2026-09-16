/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.*;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

import static com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalAirMixingGameTests.Fixture;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalTimingGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void delayedSourceCutsCoverTheWholeInterval(GameTestHelper helper) {
        double reference = Double.NaN;
        for (int interval : new int[]{20, 40, 100, 200}) {
            try (Fixture f = air(helper, new BlockPos(0, 64, 0), 8)) {
                double before = f.energy();
                var source = new ThermalSourceBatch.Builder(0);
                f.register(source, 1, 1, new BlockPos(1, 65, 1));
                f.cut(source.buildAndReset(), ResolvedGeometryBatch.EMPTY);
                for (f.tick = interval; f.tick <= 12000; f.tick += interval) f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
                near(helper, before + 6400 * 600, f.energy(), "identical delivered Air energy");
                double temperature = f.arena.temperatureC(f.slot(new BlockPos(1, 66, 1)), 0);
                if (interval == 20) reference = temperature;
                helper.assertTrue(Math.abs(temperature - reference) < 1,
                        "coalescing must not retain the old multi-second supply/one-second exchange mismatch: " + interval + ", " + temperature);
                FHMain.LOGGER.info("Thermal elapsed-time regression: intervalTicks={}, sourceC={}, referenceC={}", interval, temperature, reference);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void delayedAirAndMaterialExchangeMatchTheirElapsedTime(GameTestHelper helper) {
        try (Fixture f = air(helper, new BlockPos(0, 64, 0), 8)) {
            int a = f.slot(new BlockPos(1, 65, 1)), b = f.slot(new BlockPos(5, 65, 1));
            double conductance = f.conductance(a, b);
            f.arena.addEnthalpyJ(a, f.arena.capacityJPerK(a) * 80);
            double before = f.energy();
            f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
            near(helper, before, f.energy(), "zero elapsed time does not exchange heat");
            double expected = 80 * Math.exp(-conductance * (f.arena.inverseCapacityKPerJ(a) + f.arena.inverseCapacityKPerJ(b)) * 10);
            f.tick = 200; f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
            near(helper, expected, f.arena.temperatureC(a, 0) - f.arena.temperatureC(b, 0), "ten seconds of Air exchange");
            near(helper, before, f.energy(), "Air exchange conserves H");
        }
        BlockPos body = new BlockPos(1, 65, 1);
        try (Fixture f = new Fixture(helper, new BlockPos(0, 64, 0), 4, 4, 4,
                (x, y, z) -> body.getX() == x && body.getY() == y && body.getZ() == z ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 256, false)) {
            int a = f.slot(body.above()), b = f.slot(body);
            double conductance = 6 * f.profiles.materials().profileOrNull(f.arena.materialProfileId(b)).faceConductanceWPerK();
            f.arena.addEnthalpyJ(a, f.arena.capacityJPerK(a) * 80);
            double before = f.energy();
            double expected = 80 * Math.exp(-conductance * (f.arena.inverseCapacityKPerJ(a) + f.arena.inverseCapacityKPerJ(b)) * 10);
            f.tick = 200; f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
            near(helper, expected, f.arena.temperatureC(a, 0) - f.arena.temperatureC(b, 0), "material uses the elapsed-time coefficient");
            near(helper, before, f.energy(), "material exchange conserves H");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void delayedFarFieldExchangeUsesElapsedTime(GameTestHelper helper) {
        try (Fixture f = air(helper, new BlockPos(0, 76, 0), 4)) {
            var page = f.handles.values().iterator().next();
            short[] columns = new short[256];
            for (short i = 0; i < columns.length; i++) columns[i] = i;
            var environment = new ThermalInputBatch.PageEnvironmentUpdate(page, false, -40, columns, new byte[256]);
            var result = f.engine.process(new ThermalInputBatch(1, ++f.sequence, 0, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                    ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalSourceBatch.EMPTY,
                    new ThermalInputBatch.PageEnvironmentUpdate[]{environment}, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN));
            helper.assertTrue(result.status() == ThermalCompletion.Status.COMPLETED, "sky update");
            int slot = f.slot(new BlockPos(1, 77, 1));
            var boundaries = f.solver.fragment(page.currentPublication().workerPageSlot() * 64 + 48).farBoundaries();
            double conductance = 0;
            for (int i = 0; i < boundaries.size(); i++) conductance += boundaries.baseConductance(i);
            helper.assertTrue(conductance > 0, "fixture exposes a real FarField boundary");
            f.arena.addEnthalpyJ(slot, f.arena.capacityJPerK(slot) * 60);
            double expected = -40 + 60 * Math.exp(-conductance * f.arena.inverseCapacityKPerJ(slot) * 10);
            f.tick = 200; f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
            near(helper, expected, f.arena.temperatureC(slot, 0), "FarField cools for the full ten seconds");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void delayedExchangeStopsAtTheLatentAckBoundary(GameTestHelper helper) {
        BlockPos ice = new BlockPos(1, 65, 1);
        try (Fixture f = new Fixture(helper, new BlockPos(0, 64, 0), 4, 4, 4,
                (x, y, z) -> ice.getX() == x && ice.getY() == y && ice.getZ() == z ? Blocks.ICE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 256, false)) {
            int body = f.slot(ice), air = f.slot(ice.above());
            var edge = f.arena.materialLaw(body).heating();
            helper.assertTrue(edge != null, "ice has a heating transition");
            double remaining = Math.min(1000, (edge.targetEnthalpyJ() - edge.sourceEnthalpyJ()) / 4);
            f.arena.acceptExternalEnergyJ(body, edge.targetEnthalpyJ() - remaining - f.arena.enthalpyJ(body));
            f.arena.setEnthalpyJ(air, f.arena.capacityJPerK(air) * (edge.transitionTemperatureC() + 100));
            double before = f.energy();
            f.tick = 200;
            var result = f.process(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
            helper.assertTrue(result.status() == ThermalCompletion.Status.COMPLETED && result.phaseRequests().length == 1, "one completed transition is offered");
            near(helper, edge.targetEnthalpyJ(), f.arena.enthalpyJ(body), "large dt stops exactly at the ACK energy boundary");
            near(helper, edge.transitionTemperatureC(), f.arena.temperatureC(body, 0), "latent plateau is not skipped");
            near(helper, before, f.energy(), "excess heat remains in the other node");
            f.tick = 400; f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
            near(helper, edge.targetEnthalpyJ(), f.arena.enthalpyJ(body), "waiting for the world ACK does not absorb extra latent energy");
        }
        helper.succeed();
    }

    private static Fixture air(GameTestHelper helper, BlockPos origin, int width) {
        return new Fixture(helper, origin, width, 4, 4, (x, y, z) -> Blocks.AIR.defaultBlockState(), 256, false);
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-7 * Math.max(1, Math.abs(expected)), message + ": " + actual + " expected " + expected);
    }
}
