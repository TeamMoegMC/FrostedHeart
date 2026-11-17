/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** One producer cut edits distant bases on the same physical boundary. */
@GameTestHolder("frostedheart_geometry_cut")
@PrefixGameTestTemplate(false)
public final class ThermalGeometryCutGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_empty_visibility", timeoutTicks = 100)
    public static void emptyBrickVisibilityDoesNotPublishAnyArenaWord(GameTestHelper helper) {
        var arena = new com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena(64);
        var token = new com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken();
        var empty = com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan.EMPTY;
        arena.stageVisibility(empty, token, true); token.commit(); arena.finishVisibility(empty, token, true);
        var cancelled = new com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken();
        arena.stageVisibility(empty, cancelled, true); cancelled.cancel(); arena.finishVisibility(empty, cancelled, true);
        helper.assertTrue(arena.highWaterMark() == 0 && arena.liveCellCount() == 0 && arena.nextLiveSlot(0) == -1,
                "A legitimate empty Brick changes no allocation or visibility bits, on commit or cancellation");
        FHMain.LOGGER.info("THERMAL_EMPTY_VISIBILITY,committedAndCancelled=true,highWater=0,live=0");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_cut_capacity", timeoutTicks = 400)
    public static void retirementInTheSameCutFinishesBeforeUnfundedDistantGrowth(GameTestHelper helper) {
        var level = helper.getLevel(); var base = new BlockPos(354304, 192, 352256); var next = base.east(64);
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false)) {
            var oldPage = simulation.page(base); long section = SectionPos.asLong(next);
            var newPage = new ThermalPageHandle(section, 1); simulation.pages.put(section, newPage);
            long mask = 0xffff; byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(newPage, 0, mask, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), mask), 0, sky, null, null);
            boolean[] retiredBeforeGrowth = {false};
            try (var occupied = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(occupied != null, "Hold the optional memory needed by the distant admission");
                var previous = ThermalDiagnostics.observer;
                ThermalDiagnostics.observer = new ThermalDiagnostics.Observer() {
                    @Override public void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean completed) {
                        if (stage.equals("PUBLISHED") && input.retirements().length != 0)
                            retiredBeforeGrowth[0] = !simulation.fixed.hasSection(oldPage.sectionKey()) && !simulation.fixed.hasSection(section);
                    }
                };
                try {
                    var input = new ThermalInputBatch(1, ++simulation.sequence, 0,
                            new ThermalInputBatch.PageAdmission[] {admission},
                            new ThermalInputBatch.PageRetirement[] {new ThermalInputBatch.PageRetirement(oldPage)},
                            ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                            ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
                    simulation.engine.offerInput(new ThermalInputEnvelope(input, new SpatialSourceTimeline.Window[0]));
                    for (int round = 0; round < 512 && simulation.engine.hasReadyWork(); round++) {
                        simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
                    }
                } finally { ThermalDiagnostics.observer = previous; }
                helper.assertTrue(retiredBeforeGrowth[0] && !simulation.fixed.hasSection(oldPage.sectionKey()),
                        "Retirement must publish while an unrelated admission in the same original cut lacks capacity");
            }
            RegionalEngineTestDriver.submit(helper, simulation.engine, batch(simulation, ThermalInputBatch.NO_ADMISSIONS, ResolvedGeometryBatch.EMPTY));
            helper.assertTrue(simulation.fixed.hasSection(section) && !simulation.engine.hasWork(),
                    "The original queued admission resumes after capacity returns, with no duplicated retirement");
            FHMain.LOGGER.info("THERMAL_GEOMETRY_CUT_CAPACITY,retiredBeforeGrowth=true,sameCut=true,admissionResumed=true");
        } finally { MinecraftThermalInput.closeActiveLevel(level); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_cut_order", timeoutTicks = 400)
    public static void differentTransportCutsKeepTheIntermediateMaterialAndOrderedEnergy(GameTestHelper helper) {
        materialCuts(helper, true);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_cut_coalesced_order", timeoutTicks = 400)
    public static void coalescedTransportKeepsTheOrderedConversionEnergy(GameTestHelper helper) {
        materialCuts(helper, false);
        helper.succeed();
    }

    private static void materialCuts(GameTestHelper helper, boolean transferred) {
        var level = helper.getLevel(); var base = new BlockPos(transferred ? 353280 : 355328, 192, 352256); var body = base.offset(1, 1, 1);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(body, Blocks.STONE.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false)) {
            var sample = new TiledThermalField.Sample(); simulation.fixed.read(body.asLong(), true, sample);
            var stoneLaw = sample.law();
            simulation.material.applyMaterialControl(simulation.slot(body), stoneLaw.enthalpyAtTemperature(32), stoneLaw.initialBranch(32));
            int stone = simulation.profiles.states().signatureId(Blocks.STONE.defaultBlockState());
            var dry = Blocks.OAK_FENCE.defaultBlockState();
            var wet = dry.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
            int wood = simulation.profiles.states().signatureId(dry);
            int full = simulation.profiles.states().signatureId(wet);
            var woodLaw = simulation.profiles.materials().profileOrNull(simulation.profiles.signatures().materialProfileId(wood)).thermalLaw();
            var fullLaw = simulation.profiles.materials().profileOrNull(simulation.profiles.signatures().materialProfileId(full)).thermalLaw();
            helper.assertTrue(simulation.profiles.signatures().sameMaterialType(wood, full) && fullLaw.capacityJPerK() > woodLaw.capacityJPerK(),
                    "The actual model adds cold water capacity to the same fence type");
            var page = simulation.page(body);
            var conversion = new ResolvedGeometryBatch.Builder();
            conversion.addResolvedCenter(page, page.beginGeometryMutation(), local(body), wood);
            conversion.addMaterialChange(page, local(body), stone, wood, ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION);
            var first = batch(simulation, ThermalInputBatch.NO_ADMISSIONS, conversion.buildAndReset());
            var mass = new ResolvedGeometryBatch.Builder();
            mass.addResolvedCenter(page, page.beginGeometryMutation(), local(body), full);
            mass.addMaterialChange(page, local(body), wood, full, ResolvedGeometryBatch.MaterialChanges.MASS_CHANGE);
            level.setBlockAndUpdate(body, wet);
            var second = batch(simulation, ThermalInputBatch.NO_ADMISSIONS, mass.buildAndReset());
            int[] commits = {0}; boolean[] intermediate = {false}; var previous = ThermalDiagnostics.observer;
            ThermalDiagnostics.observer = new ThermalDiagnostics.Observer() {
                @Override public void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean completed) {
                    if (!stage.equals("PUBLISHED")) return;
                    commits[0]++;
                    if (input.sequence() == first.sequence()) intermediate[0] = simulation.fixed.read(body.asLong(), true, sample)
                            && sample.law() == woodLaw && Math.abs(sample.temperatureC() - 32) < 1e-9;
                }
            };
            try {
                simulation.engine.offerInput(new ThermalInputEnvelope(first, new SpatialSourceTimeline.Window[0]));
                if (transferred) {
                    com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyIngress ingress =
                            ThermalLoadedWorldGameTests.read(simulation.engine, "topologyIngress");
                    for (int round = 0; round < 256 && ingress.hasWork(); round++) simulation.engine.processWorkRound();
                    helper.assertTrue(!ingress.hasWork(), "Transfer the first original cut before the next cut arrives; untransferred descriptions may coalesce");
                }
                RegionalEngineTestDriver.submit(helper, simulation.engine, second);
            } finally { ThermalDiagnostics.observer = previous; }
            double expected = 32 * woodLaw.capacityJPerK() / fullLaw.capacityJPerK();
            simulation.fixed.read(body.asLong(), true, sample);
            FHMain.LOGGER.info("THERMAL_GEOMETRY_CUT_ORDER,transferred={},commits={},intermediate={},expectedFinalC={},actualFinalC={},physicalTick={}",
                    transferred, commits[0], intermediate[0], expected, sample.temperatureC(), sample.sampleTick());
            if (transferred) helper.assertTrue(commits[0] == 2 && intermediate[0], "Distinct coordinator cuts preserve the intermediate warm wood even at the same physical tick");
            helper.assertTrue(simulation.fixed.read(body.asLong(), true, sample) && Math.abs(sample.temperatureC() - expected) < 1e-9
                            && sample.sampleTick() == 0, "The later mass change preserves the converted energy on the original clock; expected=" + expected + ", actual=" + sample.temperatureC());
        } finally { MinecraftThermalInput.closeActiveLevel(level); }
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_cut", timeoutTicks = 400)
    public static void oneCutPreservesAllDistantBodiesWithOneGeometryCommit(GameTestHelper helper) {
        var level = helper.getLevel();
        var first = new BlockPos(352256, 192, 352256);
        var bases = new BlockPos[] {first, first.east(64), first.east(128)};
        for (var base : bases) {
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(base.offset(1, 1, 1), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(base.offset(2, 1, 1), Blocks.STONE.defaultBlockState());
        }
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, first, 4, 4, 4, 0, false)) {
            var admissions = new ThermalInputBatch.PageAdmission[2];
            for (int index = 1; index < bases.length; index++) {
                long section = SectionPos.asLong(bases[index]);
                var page = new ThermalPageHandle(section, 1); simulation.pages.put(section, page);
                byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
                admissions[index - 1] = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                        simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            }
            RegionalEngineTestDriver.submit(helper, simulation.engine, batch(simulation, admissions, ResolvedGeometryBatch.EMPTY));
            var sample = new TiledThermalField.Sample();
            for (var base : bases) {
                var body = base.offset(1, 1, 1); simulation.fixed.read(body.asLong(), true, sample);
                simulation.material.applyMaterialControl(simulation.slot(body), sample.law().enthalpyAtTemperature(32), sample.law().initialBranch(32));
            }
            double before = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            int[] commits = {0};
            var previous = ThermalDiagnostics.observer;
            ThermalDiagnostics.observer = new ThermalDiagnostics.Observer() {
                @Override public void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean completed) {
                    if (stage.equals("PUBLISHED")) commits[0]++;
                }
            };
            try {
                var geometry = new ResolvedGeometryBatch.Builder();
                int stone = simulation.profiles.states().signatureId(Blocks.STONE.defaultBlockState());
                int air = simulation.profiles.states().signatureId(Blocks.AIR.defaultBlockState());
                for (var base : bases) {
                    var wall = base.offset(2, 1, 1); var page = simulation.page(wall);
                    level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
                    geometry.addResolvedCenter(page, page.beginGeometryMutation(), local(wall), air);
                    geometry.addMaterialChange(page, local(wall), stone, air, ResolvedGeometryBatch.MaterialChanges.REPLACE);
                }
                RegionalEngineTestDriver.submit(helper, simulation.engine, batch(simulation, ThermalInputBatch.NO_ADMISSIONS, geometry.buildAndReset()));
            } finally { ThermalDiagnostics.observer = previous; }
            for (var base : bases) {
                helper.assertTrue(simulation.fixed.read(base.offset(1, 1, 1).asLong(), true, sample)
                                && Math.abs(sample.temperatureC() - 32) < 1e-9 && sample.sampleTick() == 0,
                        "A geometry-only cut preserves each distant warm body and its actual physical time");
                helper.assertTrue(simulation.fixed.sampleAir(base.getX() + 2.5, base.getY() + 1.5, base.getZ() + 1.5, sample),
                        "Every distant wall removal is installed through the real queryable field");
            }
            helper.assertTrue(Math.abs(before - simulation.fixed.totalEnthalpyJ() - simulation.fixed.externalEnergyJ()) < 1e-7,
                    "One geometry cut neither consumes source intervals nor duplicates/removes warm energy");
            FHMain.LOGGER.info("THERMAL_GEOMETRY_CUT,bases=3,geometryCommits={},bodyC=32,physicalTick=0,conservedJ={}", commits[0], before);
            helper.assertTrue(commits[0] == 1, "One producer cut must not repeat dimension-wide zero-time commits for each base; actual=" + commits[0]);
        } finally { MinecraftThermalInput.closeActiveLevel(level); }
        helper.succeed();
    }

    private static ThermalInputBatch batch(ThermalMaterialBehaviorGameTests.Simulation simulation,
            ThermalInputBatch.PageAdmission[] admissions, ResolvedGeometryBatch geometry) {
        return new ThermalInputBatch(1, ++simulation.sequence, 0, admissions, ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES, geometry, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
    }
    private static int local(BlockPos position) { return (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8; }
}
