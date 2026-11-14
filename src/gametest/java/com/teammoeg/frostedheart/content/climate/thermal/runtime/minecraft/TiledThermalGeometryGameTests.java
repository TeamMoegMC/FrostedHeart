/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual fixed-engine heat transfer and the geometry it still shares with the regional control. */
@GameTestHolder("frostedheart_tiled_geometry")
@PrefixGameTestTemplate(false)
public final class TiledThermalGeometryGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_faces", timeoutTicks = 600)
    public static void materialAndAirStillExchangeHeatWithoutDuplicateNumericalGraphs(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(352256, 192, 352256);
        var hot = origin.offset(2, 1, 1); var cold = hot.east(); var air = hot.above();
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), (y == 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        level.setBlockAndUpdate(hot, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(cold, Blocks.STONE.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 8, 4, 4, 0, true)) {
            simulation.acceptEnergyJ(simulation.slot(hot), simulation.arena.materialLaw(simulation.slot(hot)).enthalpyAtTemperature(32));
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            double initial = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            for (int tick = 20; tick <= 120; tick += 20) advance(helper, simulation, tick, ResolvedGeometryBatch.EMPTY);
            var material = new MaterialSample(); var sample = new TiledThermalField.Sample();
            simulation.material.readMaterial(simulation.slot(hot), material); double hotC = material.temperatureC();
            simulation.material.readMaterial(simulation.slot(cold), material); double coldC = material.temperatureC();
            helper.assertTrue(hotC > 0 && hotC < 32 && coldC > 0, "Adjacent materials still exchange real heat through the existing engine");
            helper.assertTrue(simulation.fixed.read(air.asLong(), false, sample) && sample.temperatureC() > 0 && sample.sampleTick() == 120,
                    "The same material heats Air without a regional numerical graph");
            near(helper, initial, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Faces and sky retain one conserved ledger");
            int airPairs = 0, materialPairs = 0, sky = 0;
            ThermalSolver solver = ThermalLoadedWorldGameTests.read(simulation.engine, "solver");
            for (var page : simulation.pages.values()) for (int brick = 0; brick < 64; brick++) {
                var fragment = solver.fragment(page.currentPublication().workerPageSlot() * 64 + brick);
                airPairs += fragment.airPairs().size(); materialPairs += fragment.materialContacts().size(); sky += fragment.farBoundaries().patchCount();
            }
            FHMain.LOGGER.info("THERMAL_FIXED_GEOMETRY_FACES,hotC={},coldC={},airC={},initialJ={},finalJ={},legacyAirPairs={},legacyMaterialPairs={},skyPatches={}",
                    hotC, coldC, sample.temperatureC(), initial, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), airPairs, materialPairs, sky);
            helper.assertTrue(sky > 0, "The fixture retains actual exposed-sky geometry");
            helper.assertTrue(airPairs == 0 && materialPairs == 0, "Fixed direct face exchange must not also build unused regional Air/material edge arrays");
        } finally {
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_route_witness", timeoutTicks = 900)
    public static void fixedCompilationKeepsTheSamePassageWitnessesAcrossBlockingAndReopening(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(356364, 192, 356352); var passage = origin.offset(5, 1, 1);
        int firstChunk = origin.getX() >> 4, lastChunk = (origin.getX() + 11) >> 4;
        for (int x = firstChunk; x <= lastChunk; x++) level.setChunkForced(x, origin.getZ() >> 4, true);
        for (int x = 0; x < 12; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
            boolean wall = x == 0 || x == 11 || x == 5 || y == 0 || y == 3 || z == 0 || z == 3;
            level.setBlockAndUpdate(origin.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        level.setBlockAndUpdate(passage, Blocks.STONE_SLAB.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
        try (var regional = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 12, 4, 4, 0);
                var fixed = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 12, 4, 4, 0, false)) {
            int comparisons = compareRoutes(helper, regional, fixed);
            helper.assertTrue(comparisons > 0 && fixed.fixed.activeRouteCount() > 0, "Both engines compile an actual cross-Section massless passage");
            var previous = Blocks.STONE_SLAB.defaultBlockState();
            for (int edit = 0; edit < 2; edit++) {
                var next = (edit == 0 ? Blocks.STONE : Blocks.STONE_SLAB).defaultBlockState();
                regional.publish(regional.change(passage, previous, next, ResolvedGeometryBatch.MaterialChanges.REPLACE), ThermalInputBatch.NO_PHASE_ACKS);
                fixed.publish(fixed.change(passage, previous, next, ResolvedGeometryBatch.MaterialChanges.REPLACE), ThermalInputBatch.NO_PHASE_ACKS);
                int contacts = compareRoutes(helper, regional, fixed); comparisons += contacts;
                helper.assertTrue(edit == 0 ? fixed.fixed.activeRouteCount() == 0 : contacts > 0 && fixed.fixed.activeRouteCount() > 0,
                        "Blocking removes the same route and reopening restores it through ordinary geometry publication");
                previous = next;
            }
            FHMain.LOGGER.info("THERMAL_FIXED_GEOMETRY_ROUTES,comparedContacts={},blockedAndReopened=true", comparisons);
        } finally {
            MinecraftThermalInput.closeActiveLevel(level);
            for (int x = firstChunk; x <= lastChunk; x++) level.setChunkForced(x, origin.getZ() >> 4, false);
        }
        helper.succeed();
    }

    private static int compareRoutes(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation regional,
            ThermalMaterialBehaviorGameTests.Simulation fixed) {
        ThermalSolver original = ThermalLoadedWorldGameTests.read(regional.engine, "solver");
        ThermalSolver reduced = ThermalLoadedWorldGameTests.read(fixed.engine, "solver");
        int comparisons = 0;
        for (long section : regional.pages.keySet()) for (int brick = 0; brick < 64; brick++) {
            var before = original.fragment(regional.pages.get(section).currentPublication().workerPageSlot() * 64 + brick).routedContacts();
            var after = reduced.fragment(fixed.pages.get(section).currentPublication().workerPageSlot() * 64 + brick).routedContacts();
            helper.assertTrue(before.size() == after.size(), "Both compilers retain exactly the same route contacts");
            for (int edge = 0; edge < before.size(); edge++) {
                near(helper, before.conductance(edge), after.conductance(edge), "Passage conductance is independent of unused local face graphs");
                for (int axis = 0; axis < 3; axis++) {
                    near(helper, regional.arena.minimum(before.first(edge), axis), fixed.arena.minimum(after.first(edge), axis), "First physical endpoint remains identical");
                    near(helper, regional.arena.minimum(before.second(edge), axis), fixed.arena.minimum(after.second(edge), axis), "Second physical endpoint remains identical");
                }
                helper.assertTrue(before.active(edge) == after.active(edge), "Route validity remains identical"); comparisons++;
            }
        }
        return comparisons;
    }

    private static void advance(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, long tick, ResolvedGeometryBatch geometry) {
        var result = RegionalEngineTestDriver.submit(helper, simulation.engine, new ThermalInputBatch(1, ++simulation.sequence, tick,
                ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, geometry,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN));
        helper.assertTrue(result.status() == com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion.Status.COMPLETED,
                "Advance through the ordinary input/engine boundary");
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-8 * Math.max(1, Math.abs(expected)), message + "; expected=" + expected + ", actual=" + actual);
    }
}
