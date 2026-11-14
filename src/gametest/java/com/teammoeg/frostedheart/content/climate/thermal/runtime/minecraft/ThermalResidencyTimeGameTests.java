/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** A request for more numerical storage must not suspend already represented physics. */
@GameTestHolder("frostedheart_residency_time")
@PrefixGameTestTemplate(false)
public final class ThermalResidencyTimeGameTests {
    private record Result(double[] temperatures, long maximumAge, double totalJ) { }

    @GameTest(template = "phase0a_empty", batch = "mixed_residency_physics", timeoutTicks = 400)
    public static void aMixedWallAndExpansionInstallsPaidPhysicsBeforeOptionalStorage(GameTestHelper helper) {
        mixedGrowth(helper, false);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "mixed_residency_physics", timeoutTicks = 400)
    public static void removingTheLastOldBrickDoesNotRequireCapacityForItsReplacement(GameTestHelper helper) {
        mixedGrowth(helper, true);
        helper.succeed();
    }

    private static void mixedGrowth(GameTestHelper helper, boolean replaceAll) {
        var base = new BlockPos(501760, 192, 491520);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            long section = SectionPos.asLong(base); var page = simulation.pages.get(section);
            submit(helper, simulation, producer, 20, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES);
            var wall = base.offset(replaceAll ? 5 : 3, 1, 2); helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            long revision = page.beginGeometryMutation();
            var geometry = new ResolvedGeometryBatch.Builder();
            geometry.addResolvedCenter(page, revision, (replaceAll ? 5 : 3) | 2 << 4 | 1 << 8, simulation.profiles.states().signatureId(Blocks.STONE.defaultBlockState()));
            long wanted = replaceAll ? 2 : 3;
            var growth = new ThermalInputBatch.PageResidencyUpdate(page, revision, wanted, replaceAll ? 2 : 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), wanted), null, null);
            try (var pressure = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(pressure != null, "Withhold optional expansion capacity");
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 35,
                        ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, new ThermalInputBatch.PageResidencyUpdate[] {growth},
                        geometry.buildAndReset(), ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(35)));
                offer(simulation, producer, 60, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES); pump(helper, simulation);
                var sample = new TiledThermalField.Sample();
                helper.assertTrue(simulation.fixed.sectionCellCount(section) == (replaceAll ? 0 : 64)
                        && simulation.fixed.sectionSampleTick(section) == 60
                        && (replaceAll ? !simulation.fixed.read(base.offset(1, 1, 1).asLong(), false, sample)
                            : simulation.fixed.read(wall.asLong(), true, sample)),
                        "Real changes and removals finish on paid space while optional demand stays queued");
            }
            offer(simulation, producer, 60, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES); pump(helper, simulation);
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(simulation.fixed.sectionCellCount(section) == (replaceAll ? 64 : 128) && simulation.fixed.read(wall.asLong(), true, sample)
                    && sample.sampleTick() == 60, "The same original Page queue later completes its expansion without changing the wall or its time");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
    }

    @GameTest(template = "phase0a_empty", batch = "residency_time", timeoutTicks = 600)
    public static void waitingGrowthPreservesTheExistingTemperatureTrajectory(GameTestHelper helper) {
        var origin = new BlockPos(491520, 192, 491520);
        var reference = run(helper, origin, false, false);
        var delayed = run(helper, origin, true, false);
        double maximumError = 0, maximumRatio = 0;
        for (int index = 0; index < reference.temperatures.length; index++) {
            double error = Math.abs(reference.temperatures[index] - delayed.temperatures[index]);
            maximumError = Math.max(maximumError, error);
            maximumRatio = Math.max(maximumRatio, error / Math.max(1, .05 * Math.abs(reference.temperatures[index])));
        }
        FHMain.LOGGER.info("THERMAL_RESIDENCY_TIME,samples={},maximumErrorC={},maximumAllowanceRatio={},maximumAgeTicks={},referenceJ={},delayedJ={}",
                reference.temperatures.length, maximumError, maximumRatio, delayed.maximumAge, reference.totalJ, delayed.totalJ);
        helper.assertTrue(maximumRatio <= 1,
                "Waiting storage preserves the real temperature trajectory within max(1C, 5% elevation); maximumErrorC="
                        + maximumError + ",allowanceRatio=" + maximumRatio);
        helper.assertTrue(delayed.maximumAge == 0, "Existing cells keep their genuine physical time while growth waits");
        near(helper, reference.totalJ, delayed.totalJ, "Both histories deliver the same actual production exactly once");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "residency_material_time", timeoutTicks = 600)
    public static void preparedGrowthCannotOverwriteMaterialsHeatedWhileItWaited(GameTestHelper helper) {
        var origin = new BlockPos(499712, 192, 499712);
        var reference = run(helper, origin, false, true);
        var delayed = run(helper, origin, true, true);
        double maximumError = 0;
        for (int index = 0; index < reference.temperatures.length; index++)
            maximumError = Math.max(maximumError, Math.abs(reference.temperatures[index] - delayed.temperatures[index]));
        FHMain.LOGGER.info("THERMAL_RESIDENCY_MATERIAL_TIME,samples={},maximumErrorC={},maximumAgeTicks={},referenceJ={},delayedJ={}",
                reference.temperatures.length, maximumError, delayed.maximumAge, reference.totalJ, delayed.totalJ);
        helper.assertTrue(maximumError <= 1e-8 && delayed.maximumAge == 0,
                "Waiting preparation preserves actual heated Air and material values, including the old/new Brick interface; errorC=" + maximumError);
        near(helper, reference.totalJ, delayed.totalJ, "Material contact and all actual source input preserve the original energy total");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "residency_physical_boundary", timeoutTicks = 400)
    public static void aLaterWallChangeBehindWaitingGrowthKeepsItsRealPhysicalBoundary(GameTestHelper helper) {
        var base = new BlockPos(495616, 192, 495616);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            long section = SectionPos.asLong(base); var page = simulation.pages.get(section);
            var h = new double[4096]; Arrays.fill(h, 32 * simulation.fixed.airCapacityJPerK());
            helper.assertTrue(simulation.fixed.restoreValues(section, h, new byte[4096]), "Start with actual warm cell energies");
            submit(helper, simulation, producer, 20, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES);
            var growth = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 3, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 3), null, null);
            try (var pressure = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(pressure != null, "Hold only the optional growth capacity");
                offer(simulation, producer, 20, ThermalInputBatch.NO_ADMISSIONS, new ThermalInputBatch.PageResidencyUpdate[] {growth});
                pump(helper, simulation);
                var wall = base.offset(3, 1, 2); helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                var changed = new ResolvedGeometryBatch.Builder();
                changed.addResolvedCenter(page, page.beginGeometryMutation(), 3 | 2 << 4 | 1 << 8,
                        simulation.profiles.states().signatureId(Blocks.STONE.defaultBlockState()));
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 35,
                        ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                        changed.buildAndReset(), ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(35)));
                offer(simulation, producer, 60, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES);
                pump(helper, simulation);
                var sample = new TiledThermalField.Sample();
                helper.assertTrue(simulation.fixed.read(base.offset(1, 1, 1).asLong(), false, sample) && sample.sampleTick() == 60,
                        "The real tick-35 wall is installed on paid space and its actual physics then advances to tick 60; actual=" + sample.sampleTick());
                helper.assertTrue(simulation.fixed.read(wall.asLong(), true, sample), "The physical wall is represented despite the older unpaid expansion");
                helper.assertTrue(simulation.fixed.sectionCellCount(section) == 64,
                        "Installing the wall does not expose unpaid growth");
                FHMain.LOGGER.info("THERMAL_RESIDENCY_PHYSICAL_BOUNDARY,growthTick=20,wallTick=35,offeredTick=60,actualSampleTick={},wallInstalled=true", sample.sampleTick());
            }
            offer(simulation, producer, 60, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES); pump(helper, simulation);
            var wallSample = new TiledThermalField.Sample();
            helper.assertTrue(simulation.fixed.sectionCellCount(section) == 128 && simulation.fixed.read(base.offset(3, 1, 2).asLong(), true, wallSample),
                    "Capacity release admits the retained demand without restoring the old pre-wall snapshot");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    private static Result run(GameTestHelper helper, BlockPos origin, boolean delay, boolean materials) {
        var remote = origin.east(64);
        for (var base : new BlockPos[] {origin, remote}) {
            for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(base.offset(1, 0, 1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            if (materials) {
                helper.getLevel().setBlockAndUpdate(base.offset(3, 1, 2), Blocks.STONE.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(base.offset(4, 1, 2), Blocks.IRON_BLOCK.defaultBlockState());
            }
        }
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            long section = SectionPos.asLong(remote);
            var page = new ThermalPageHandle(section, 1); simulation.pages.put(section, page);
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            var profile = simulation.profiles.tuning().campfire();
            for (var base : new BlockPos[] {origin, remote}) {
                var fire = base.offset(1, 0, 1);
                helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                        fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                        profile.ratedPowerW(), true, 0), "Register the actual physical source profile");
            }
            submit(helper, simulation, producer, 0, new ThermalInputBatch.PageAdmission[] {admission}, ThermalInputBatch.NO_RESIDENCY_UPDATES);
            submit(helper, simulation, producer, 20, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES);
            var growth = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 3, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 3), null, null);
            var pressure = delay ? simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048) : null;
            double[] temperatures = new double[40 * 4 + 127]; int at = 0; long maximumAge = 0;
            try {
                if (delay) {
                    helper.assertTrue(pressure != null, "Fill the growth budget without allocating a synthetic heap object");
                    offer(simulation, producer, 20, ThermalInputBatch.NO_ADMISSIONS, new ThermalInputBatch.PageResidencyUpdate[] {growth});
                    pump(helper, simulation);
                }
                var sample = new TiledThermalField.Sample();
                for (int cut = 2; cut <= 41; cut++) {
                    long tick = cut * 20L;
                    offer(simulation, producer, tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES);
                    pump(helper, simulation);
                    helper.assertTrue(simulation.fixed.sectionSampleTick(SectionPos.asLong(origin)) == tick,
                            "An unrelated base keeps advancing during the same capacity pressure");
                    maximumAge = Math.max(maximumAge, tick - simulation.fixed.sectionSampleTick(section));
                    for (int x = 0; x < 4; x++) {
                        helper.assertTrue(simulation.fixed.sampleAir(remote.getX() + x + .5, remote.getY() + 1.5, remote.getZ() + 1.5, sample),
                                "Read genuine spatial samples while the additional Brick is absent");
                        temperatures[at++] = sample.temperatureC();
                    }
                }
            } finally { if (pressure != null) pressure.close(); }
            if (!delay) offer(simulation, producer, 820, ThermalInputBatch.NO_ADMISSIONS, new ThermalInputBatch.PageResidencyUpdate[] {growth});
            for (int flush = 0; flush < 4; flush++) {
                offer(simulation, producer, 820, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RESIDENCY_UPDATES);
                pump(helper, simulation);
            }
            helper.assertTrue(simulation.fixed.sectionCellCount(section) == 128, "Both histories eventually admit the same additional storage");
            var sample = new TiledThermalField.Sample();
            for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                if (x == 1 && y == 0 && z == 1) continue;
                boolean material = materials && y == 1 && z == 2 && (x == 3 || x == 4);
                helper.assertTrue(simulation.fixed.read(remote.offset(x, y, z).asLong(), material, sample), "Read every final canonical cell");
                temperatures[at++] = sample.temperatureC();
            }
            return new Result(temperatures, maximumAge, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally {
            for (var base : new BlockPos[] {origin, remote}) helper.getLevel().setBlockAndUpdate(base.offset(1, 0, 1), Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        }
    }

    private static void submit(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, SpatialSourceTimeline producer,
            long tick, ThermalInputBatch.PageAdmission[] admissions, ThermalInputBatch.PageResidencyUpdate[] residency) {
        offer(simulation, producer, tick, admissions, residency); pump(helper, simulation);
    }
    private static void offer(ThermalMaterialBehaviorGameTests.Simulation simulation, SpatialSourceTimeline producer,
            long tick, ThermalInputBatch.PageAdmission[] admissions, ThermalInputBatch.PageResidencyUpdate[] residency) {
        simulation.tick = tick;
        simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, tick, admissions,
                ThermalInputBatch.NO_RETIREMENTS, residency, ResolvedGeometryBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(tick)));
    }
    private static void pump(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation) {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (simulation.engine.hasReadyWork()) {
            helper.assertTrue(System.nanoTime() < deadline, "A capacity wait parks without coordinator spin");
            simulation.engine.processWorkRound();
            while (simulation.engine.pollWorkCompletion() != null) { }
        }
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-8 * Math.max(1, Math.abs(expected)), message + ";expected=" + expected + ",actual=" + actual);
    }
}
