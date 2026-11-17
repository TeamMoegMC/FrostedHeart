/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Original admission, checkpoint and cold-input messages drive every numerical step. */
@GameTestHolder("frostedheart_tiled_restore")
@PrefixGameTestTemplate(false)
public final class TiledThermalRestoreGameTests {
    private static final double[][] PROBES = {{.125, .25, .75}, {.5, .5, .5}, {.875, .75, .25}};

    @GameTest(template = "phase0a_empty", batch = "fixed_native_growth_restore", timeoutTicks = 800)
    public static void restoringNewBricksKeepsLiveHeatAndImportsTheSavedContributionOnce(GameTestHelper helper) {
        var origin = new BlockPos(241664, 192, 241664); var fire = origin.offset(3, 0, 1); var iron = origin.offset(5, 1, 2);
        fill(helper, origin, 8); helper.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        helper.getLevel().setBlockAndUpdate(iron, Blocks.IRON_BLOCK.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        DormantChunkThermalState saved;
        try (var writer = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 8, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(writer.budget.createCategory(), 16)) {
            var profile = writer.profiles.tuning().campfire();
            helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                    profile.ratedPowerW(), true, 0), "Use an actual face source shared by the two Bricks");
            submit(helper, writer, producer, 0); submit(helper, writer, producer, 20); submit(helper, writer, producer, 40);
            var material = new MaterialSample(); writer.material.readMaterial(writer.slot(iron), material);
            writer.material.applyMaterialControl(writer.slot(iron), material.law().enthalpyAtTemperature(32), material.law().initialBranch(32));
            saved = capture(helper, writer, producer, new ChunkPos(origin).toLong(), 40);
        }
        try (var reader = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false,
                40, chunk -> saved, Double.POSITIVE_INFINITY)) {
            var extra = new SpatialHeatInput.Builder();
            extra.appendAggregate(new SpatialHeatInput.Target(origin.offset(1, 1, 1).asLong(), (byte) 2, -1, 0, 0, 0, 0, 0), 40, 60, 1200);
            reader.engine.offerRestoredHeat(new ChunkPos(origin).toLong(), PendingHeatCheckpoint.capture(extra.freeze(), ignored -> -1, 40), 40, Double.POSITIVE_INFINITY, 1);
            reader.tick = 60; reader.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            double before = reader.fixed.totalEnthalpyJ(), imported = 0;
            var entry = saved.air(12); var spatial = entry.spatialAir();
            helper.assertTrue(entry.hasBrick(1), "The native fixture must retain actual saved Air on the new side of the shared boundary");
            for (int cell = 0; cell < 64; cell++) {
                double temperature = spatial.blockCenterC(1, cell);
                if (Double.isFinite(temperature)) imported += temperature * reader.profiles.tuning().airHeatCapacityJPerBlockK();
            }
            var materials = saved.materials(12);
            for (int body = 0; body < materials.size(); body++) if ((materials.position(body) & 15) >= 4) imported += materials.enthalpyJ(body);
            var page = reader.page(origin);
            var update = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 3, 3,
                    reader.capture.captureBricks(page.sectionKey(), null, reader.capture.unresolvedPage(), 3),
                    new ThermalInputBatch.DormantAirCut(entry, 0, 1, 60, 0),
                    new ThermalInputBatch.DormantMaterialCut(materials, 60, 0, 0));
            RegionalEngineTestDriver.submit(helper, reader.engine, residency(reader, update));
            near(helper, before + imported, reader.fixed.totalEnthalpyJ(), "Joining capacity preserves live heat and adds only the newly restored contribution");
            RegionalEngineTestDriver.submit(helper, reader.engine, residency(reader, update));
            near(helper, before + imported, reader.fixed.totalEnthalpyJ(), "Repeated residency data cannot restore either Brick a second time");
            FHMain.LOGGER.info("THERMAL_FIXED_NATIVE_GROWTH_RESTORE,beforeJ={},newSavedJ={},afterJ={},liveExtraJ=1200,repeatedImport=false,sharedCornerConserved=true", before, imported, reader.fixed.totalEnthalpyJ());
        } finally { helper.getLevel().setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_native_value_restore", timeoutTicks = 600)
    public static void admissionRestoresNativeGradientsAndMaterialCoolingOnce(GameTestHelper helper) {
        var origin = new BlockPos(229376, 192, 229376); var fire = origin.offset(1, 0, 1); var iron = origin.offset(3, 1, 2);
        fill(helper, origin, 4); helper.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        helper.getLevel().setBlockAndUpdate(iron, Blocks.IRON_BLOCK.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        DormantChunkThermalState saved;
        double[] before = new double[PROBES.length];
        try (var writer = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(writer.budget.createCategory(), 16)) {
            var profile = writer.profiles.tuning().campfire();
            helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                    profile.ratedPowerW(), true, 0), "Register the actual campfire profile on the original producer");
            submit(helper, writer, producer, 0); submit(helper, writer, producer, 20);
            var material = new MaterialSample(); writer.material.readMaterial(writer.slot(iron), material);
            writer.material.applyMaterialControl(writer.slot(iron), material.law().enthalpyAtTemperature(32), material.law().initialBranch(32));
            for (int i = 0; i < before.length; i++) before[i] = air(helper, writer, fire.above(), PROBES[i]);
            saved = capture(helper, writer, producer, new ChunkPos(origin).toLong(), 20);
        }
        try (var reader = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false,
                60, chunk -> saved, 2)) {
            for (int i = 0; i < before.length; i++) near(helper, before[i] * .5, air(helper, reader, fire.above(), PROBES[i]),
                    "Native admission imports each fractional gradient and applies the two-second half-life once");
            var material = new MaterialSample(); reader.material.readMaterial(reader.slot(iron), material);
            near(helper, 16, material.temperatureC(), "The actual native material cut cools 32C to 16C");
            double first = air(helper, reader, fire.above(), PROBES[0]);
            reader.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            near(helper, first, air(helper, reader, fire.above(), PROBES[0]), "Repeated empty cuts do not restore or cool the field again");
            reader.publish(reader.change(iron, Blocks.IRON_BLOCK.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(),
                    ResolvedGeometryBatch.MaterialChanges.REPLACE), ThermalInputBatch.NO_PHASE_ACKS);
            reader.material.readMaterial(reader.slot(iron), material);
            near(helper, 0, material.temperatureC(), "Replacing the same BlockState cannot restore the archived body's heat again");
            FHMain.LOGGER.info("THERMAL_FIXED_NATIVE_VALUE_RESTORE,savedTick=20,restoredTick=60,halfLifeSeconds=2,gradientSamples={},materialBeforeC=32,materialRestoredC=16,replacementC={},originalAdmissions=true",
                    before.length, material.temperatureC());
        } finally { helper.getLevel().setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_cold_identity", timeoutTicks = 600)
    public static void restoredMaterialInputPinsTheActualBodyBeforeASameStateReplacement(GameTestHelper helper) {
        var origin = new BlockPos(233472, 192, 233472); var iron = origin.offset(3, 1, 2); var receiver = origin.offset(1, 1, 1);
        fill(helper, origin, 4); helper.getLevel().setBlockAndUpdate(iron, Blocks.IRON_BLOCK.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false)) {
            var heat = new SpatialHeatInput.Builder();
            heat.appendAggregate(new SpatialHeatInput.Target(receiver.asLong(), (byte) 2, -1, 0, 0, 0, 0, 0), 0, 20, 6400);
            heat.appendAggregate(materialTarget(iron), 0, 20, 1600);
            var pending = PendingHeatCheckpoint.capture(heat.freeze(), ignored -> -1, 0);
            helper.assertTrue(simulation.engine.offerRestoredHeat(new ChunkPos(origin).toLong(), pending, 0, Double.POSITIVE_INFINITY, 1), "Transfer the native finite input once");
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (!restoreRequests(simulation).isEmpty()) {
                helper.assertTrue(simulation.engine.hasReadyWork() && System.nanoTime() < deadline, "The fixed engine must service the original restore queue");
                simulation.engine.processWorkRound();
            }
            simulation.publish(simulation.change(iron, Blocks.IRON_BLOCK.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(),
                    ResolvedGeometryBatch.MaterialChanges.REPLACE), ThermalInputBatch.NO_PHASE_ACKS);
            simulation.tick = 20; simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            ThermalSourceLedger ledger = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
            near(helper, 6400, ledger.energyBalance().deliveredJ(), "Only the Air archive is physically delivered");
            near(helper, 1600, ledger.energyBalance().unacceptedJ(), "The old material archive cannot heat its replacement");
            near(helper, 6400, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "All surviving input remains owned once");
            simulation.tick = 40; simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            near(helper, 6400, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "A later passive interval cannot replay the archive twice");
            FHMain.LOGGER.info("THERMAL_FIXED_RESTORE_INCARNATION,deliveredJ={},unacceptedJ={},finalJ={},sameStateReplacement=true", ledger.energyBalance().deliveredJ(), ledger.energyBalance().unacceptedJ(), simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_cold_geometry", timeoutTicks = 600)
    public static void aWaitingRestoreDoesNotBlockAnotherChunkAndBindsWhenGeometryArrives(GameTestHelper helper) {
        var origin = new BlockPos(237568, 192, 237568); var delayed = origin.east(32); var iron = delayed.offset(1, 1, 1); var air = origin.offset(1, 1, 1);
        fill(helper, origin, 4); fill(helper, delayed, 4); helper.getLevel().setBlockAndUpdate(iron, Blocks.IRON_BLOCK.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false)) {
            var material = new SpatialHeatInput.Builder(); material.appendAggregate(materialTarget(iron), 0, 20, 1600);
            var direct = new SpatialHeatInput.Builder(); direct.appendAggregate(new SpatialHeatInput.Target(air.asLong(), (byte) 2, -1, 0, 0, 0, 0, 0), 0, 20, 6400);
            long delayedChunk = new ChunkPos(delayed).toLong();
            simulation.engine.offerRestoredHeat(delayedChunk, PendingHeatCheckpoint.capture(material.freeze(), ignored -> -1, 0), 0, Double.POSITIVE_INFINITY, 2);
            simulation.engine.offerRestoredHeat(new ChunkPos(origin).toLong(), PendingHeatCheckpoint.capture(direct.freeze(), ignored -> -1, 0), 0, Double.POSITIVE_INFINITY, 1);
            simulation.engine.offerInput(new ThermalInputEnvelope(input(simulation, 20, ThermalInputBatch.NO_ADMISSIONS), new SpatialSourceTimeline.Window[0]));
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (simulation.fixed.acceptedTick() < 20) {
                helper.assertTrue(simulation.engine.hasReadyWork() && System.nanoTime() < deadline, "Independent restore and numerical work must progress");
                simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
            }
            near(helper, 6400, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "A later ready restore is not blocked by the first missing geometry");
            for (int round = 0; round < 64 && simulation.engine.hasReadyWork(); round++) {
                simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
            }
            helper.assertTrue(restoreRequests(simulation).size() == 1 && !simulation.engine.hasReadyWork(), "Unavailable geometry retains its input without a runnable busy loop");
            var page = new ThermalPageHandle(SectionPos.asLong(delayed), 2); simulation.pages.put(page.sectionKey(), page);
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                    simulation.capture.captureBricks(page.sectionKey(), null, simulation.capture.unresolvedPage(), 1), 0, sky, null);
            RegionalEngineTestDriver.submit(helper, simulation.engine, input(simulation, 20, new ThermalInputBatch.PageAdmission[] {admission}));
            helper.assertTrue(restoreRequests(simulation).isEmpty(), "The original geometry message makes the deferred restore ready");
            simulation.tick = 40; simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            near(helper, 8000, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Delayed geometry delivers its archive over the next finite interval");
            FHMain.LOGGER.info("THERMAL_FIXED_RESTORE_GEOMETRY,independentChunkTick=20,delayedArrivalTick=20,completedTick=40,conservedJ={},busyWhileUnavailable=false", simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    private static java.util.ArrayDeque<?> restoreRequests(ThermalMaterialBehaviorGameTests.Simulation simulation) {
        return ThermalLoadedWorldGameTests.read(simulation.engine, "restoreRequests");
    }
    private static SpatialHeatInput.Target materialTarget(BlockPos position) {
        return new SpatialHeatInput.Target(position.asLong(), SpatialHeatInput.MATERIAL, -1, 0, 0, 0, 0, 0, Block.getId(Blocks.IRON_BLOCK.defaultBlockState()));
    }
    private static ThermalInputBatch input(ThermalMaterialBehaviorGameTests.Simulation simulation, long tick, ThermalInputBatch.PageAdmission[] admissions) {
        return new ThermalInputBatch(1, ++simulation.sequence, tick, admissions, ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
    }
    private static ThermalInputBatch residency(ThermalMaterialBehaviorGameTests.Simulation simulation, ThermalInputBatch.PageResidencyUpdate update) {
        return new ThermalInputBatch(1, ++simulation.sequence, simulation.tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                new ThermalInputBatch.PageResidencyUpdate[] {update}, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
    }
    private static void submit(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, SpatialSourceTimeline producer, long tick) {
        RegionalEngineTestDriver.submit(helper, simulation.engine, new ThermalInputEnvelope(input(simulation, tick, ThermalInputBatch.NO_ADMISSIONS), producer.seal(tick)));
    }
    private static DormantChunkThermalState capture(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation,
            SpatialSourceTimeline producer, long chunk, long tick) {
        try (var source = producer.capture(tick)) {
            var ready = simulation.engine.requestCheckpoint(chunk, source, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.SAVE);
            while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
            var checkpoint = ready.join(); var saved = new DormantChunkThermalState(12, 1);
            for (var section : checkpoint.sections()) {
                var air = section.captureAir(0);
                if (air != null) saved.replace(SectionPos.y(section.sectionKey()), new DormantChunkThermalState.SectionEntry(section.sampleTick(), 0, air));
                saved.replaceMaterials(SectionPos.y(section.sectionKey()), MaterialSectionState.capture(section, new MaterialSectionState.CaptureScratch(), 0).state());
            }
            var finished = checkpoint.finish(false); while (!finished.isDone()) simulation.engine.processCheckpointCommandOnMain();
            var tag = new CompoundTag(); saved.encode(tag);
            return DormantChunkThermalState.decode(tag, 12, 1);
        }
    }
    private static double air(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, BlockPos point, double[] offset) {
        var sample = new QueryPublication.MutableSample();
        helper.assertTrue(simulation.query.tryReadAir(point.getX() + offset[0], point.getY() + offset[1], point.getZ() + offset[2], sample), "Read an actual fractional Air position");
        return sample.temperatureC();
    }
    private static void fill(GameTestHelper helper, BlockPos origin, int width) {
        for (int x = 0; x < width; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-7 * Math.max(1, Math.abs(expected)), message + ";expected=" + expected + ",actual=" + actual);
    }
}
