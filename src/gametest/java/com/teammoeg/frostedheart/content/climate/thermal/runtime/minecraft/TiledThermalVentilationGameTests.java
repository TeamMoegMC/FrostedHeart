/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialPowerTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Reuses the real passage compiler and source outlet resolver; does not qualify the whole runtime. */
@GameTestHolder("frostedheart_tiled_ventilation")
@PrefixGameTestTemplate(false)
public final class TiledThermalVentilationGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 1200)
    public static void slabPassageTransfersHeatAndRetiresItsOldRouteWhenBlocked(GameTestHelper helper) {
        comparePassage(helper, false);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1200)
    public static void sourceInsidePassageUsesTheExistingOutletAndBlockedSink(GameTestHelper helper) {
        comparePassage(helper, true);
    }

    private static void comparePassage(GameTestHelper helper, boolean routedSource) {
        var level = helper.getLevel();
        var origin = new BlockPos(28684, 192, routedSource ? 28736 : 28672);
        var passage = origin.offset(5, 1, 1);
        var source = origin.offset(routedSource ? 5 : 2, 0, 1);
        for (int x = origin.getX() >> 4; x <= (origin.getX() + 11) >> 4; x++) level.setChunkForced(x, origin.getZ() >> 4, true);
        try {
            for (int x = 0; x < 12; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                boolean wall = x == 0 || x == 11 || x == 5 || y == 0 || y == 3 || z == 0 || z == 3;
                level.setBlockAndUpdate(origin.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
            level.setBlockAndUpdate(passage, Blocks.STONE_SLAB.defaultBlockState());
            level.setBlockAndUpdate(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            MinecraftThermalInput.closeActiveLevel(level);
            var budget = new ThermalMemoryBudget(8L << 20);
            try (var reference = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 12, 4, 4, 0);
                    var sources = new SpatialSourceTimeline(reference.budget.createCategory(), 8);
                    var banks = new SpatialEnergyBanks(budget, 8, 32, 0);
                    var powers = new SpatialPowerTimeline(budget, banks, 8, 1, 0);
                    var field = new TiledThermalField(budget, reference.profiles.signatures(), reference.profiles.materials(),
                            reference.profiles.tuning().airHeatCapacityJPerBlockK(), reference.profiles.tuning().airMixingWPerBlockK(), 0, 8, 0)) {
                for (long section : reference.pages.keySet()) {
                    int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
                    long bricks = 0;
                    for (int x = 0; x < 12; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                        var position = origin.offset(x, y, z);
                        if (SectionPos.asLong(position) != section) continue;
                        captured[local(position)] = reference.profiles.states().signatureId(level.getBlockState(position));
                        int brick = ((position.getX() & 15) >>> 2) | ((position.getZ() & 15) >>> 2) << 2 | ((position.getY() & 15) >>> 2) << 4;
                        bricks |= 1L << brick;
                    }
                    helper.assertTrue(field.admit(section, captured, bricks, 0), "Admit the actual cross-Section passage geometry");
                }
                var profile = reference.profiles.tuning().campfire();
                var descriptor = new SpatialSourceTimeline.Descriptor(source.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                        source.getX(), source.getY(), source.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(source.asLong(), profile));
                helper.assertTrue(sources.register(descriptor, profile.ratedPowerW(), true, 0), "Drive the original source profile");
                RegionalEngineTestDriver.submit(helper, reference.engine, new ThermalInputEnvelope(input(++reference.sequence, 0, ResolvedGeometryBatch.EMPTY), sources.seal(0)));
                for (long section : reference.pages.keySet()) helper.assertTrue(reference.engine.copyTransport(section, field), "Install the existing passage compilation");
                helper.assertTrue(field.activeRouteCount() > 0, "The fixture must contain an actual compiled massless Air route");
                var destination = reference.engine.sourcePortDestination(descriptor, 0);
                helper.assertTrue(destination.target() != null, "The real source outlet resolves before production");
                var receiver = new SpatialEnergyBanks.Address(destination.target().position(), -1, destination.target().face());
                helper.assertTrue(powers.tryRegister(source.asLong(), 1, new SpatialEnergyBanks.Address[] {receiver, null},
                                new double[] {profile.port(0).powerShare(), profile.port(1).powerShare()}, profile.ratedPowerW(), true, 0)
                                && field.updateMixing(source.above().asLong(), 1), "Use the resolved outlet and original mixing origin");
                double initialJ = field.totalEnthalpyJ() + field.externalEnergyJ();
                double maximumRatio = 0, maximumError = 0;
                int comparisons = 0;
                String worst = "";
                var retiredTransport = new ArrayList<WeakReference<Object>>();
                var air = new QueryPublication.MutableSample(); var matter = new MaterialSample(); var sample = new TiledThermalField.Sample();
                for (int tick = 20; tick <= 600; tick += 20) {
                    if (tick == 200 || tick == 400) {
                        rememberTransport(field, reference.pages.keySet(), retiredTransport);
                        var state = tick == 200 ? Blocks.STONE.defaultBlockState() : Blocks.STONE_SLAB.defaultBlockState();
                        var geometry = reference.change(passage, state, ResolvedGeometryBatch.MaterialChanges.REPLACE);
                        helper.assertTrue(field.activeRouteCount() == 0, "A real pending world replacement immediately invalidates the old passage witness");
                        helper.assertTrue(field.changeCell(passage.asLong(), reference.profiles.states().signatureId(state), TiledThermalField.Change.REPLACEMENT), "Capture the same actual passage replacement");
                        // Commit the geometry at the already reached instant, then advance both
                        // models over the next second under that same geometry and outlet.
                        RegionalEngineTestDriver.submit(helper, reference.engine, new ThermalInputEnvelope(input(++reference.sequence, tick - 20, geometry), sources.seal(tick - 20)));
                        for (long section : reference.pages.keySet()) helper.assertTrue(reference.engine.copyTransport(section, field), "Replace only committed local route values");
                        helper.assertTrue(tick == 200 ? field.activeRouteCount() == 0 : field.activeRouteCount() > 0, "Blocking removes routes and reopening rebuilds them");
                        destination = reference.engine.sourcePortDestination(descriptor, 0);
                        helper.assertTrue(destination.target() != null || destination.declaredLoss(), "Resolved blocked ports are declared sinks, with no unresolved geometry");
                        receiver = destination.target() == null ? null
                                : new SpatialEnergyBanks.Address(destination.target().position(), -1, destination.target().face());
                        helper.assertTrue(powers.tryRetarget(source.asLong(), 1, new SpatialEnergyBanks.Address[] {receiver, null},
                                new double[] {profile.port(0).powerShare(), profile.port(1).powerShare()}, tick - 20), "Use the existing source resolver after every world replacement");
                        step(helper, field, banks, powers, tick - 20);
                    }
                    RegionalEngineTestDriver.submit(helper, reference.engine, new ThermalInputEnvelope(input(++reference.sequence, tick, ResolvedGeometryBatch.EMPTY), sources.seal(tick)));
                    step(helper, field, banks, powers, tick);
                    for (int x = 1; x < 11; x++) for (int y = 1; y < 3; y++) for (int z = 1; z < 3; z++) {
                        var position = origin.offset(x, y, z);
                        boolean material = !level.getBlockState(position).isAir();
                        double expected;
                        long sampled;
                        if (material) {
                            reference.material.readMaterial(reference.slot(position), matter);
                            helper.assertTrue(matter.source() == MaterialSample.Source.LIVE, "Passage material has a current reference state");
                            expected = matter.temperatureC(); sampled = matter.sampleTick();
                        } else {
                            helper.assertTrue(reference.query.tryReadAir(position.getX() + .5, position.getY() + .5, position.getZ() + .5, air), "Both sides of the passage have live reference Air");
                            expected = air.temperatureC(); sampled = air.sampleTick();
                        }
                        helper.assertTrue(field.read(position.asLong(), material, sample) && sampled == tick && sample.sampleTick() == tick, "Compare actual positions at the same physical instant");
                        double error = Math.abs(expected - sample.temperatureC()), ratio = error / Math.max(1, .05 * Math.abs(expected));
                        if (ratio > maximumRatio) { maximumRatio = ratio; worst = "tick=" + tick + ",x=" + x + ",y=" + y + ",z=" + z + ",old=" + expected + ",new=" + sample.temperatureC(); }
                        maximumError = Math.max(maximumError, error); comparisons++;
                    }
                    double acceptedJ = powers.producedJ() - powers.declaredLossJ();
                    double ownedJ = field.totalEnthalpyJ() + field.externalEnergyJ() - initialJ;
                    helper.assertTrue(Math.abs(ownedJ - acceptedJ) <= 1e-6 * Math.max(1, acceptedJ), "Massless routes transfer each J between endpoints without storing or duplicating it");
                }
                FHMain.LOGGER.info("THERMAL_TILED_VENTILATION_RESULT,routedSource={},comparisons={},maximumErrorC={},maximumAllowanceRatio={},activeRoutes={},peakReservedBytes={},worst={}",
                        routedSource, comparisons, maximumError, maximumRatio, field.activeRouteCount(), budget.peakUsedBytes(), worst);
                helper.assertTrue(Double.isFinite(maximumError), "Passage accuracy is recorded; conservation and routing remain correctness gates");
                // Explicit diagnostic collection is outside the scale/performance windows.
                // Keep the heated core alive while checking its discarded transport owners.
                System.gc(); System.gc();
                helper.assertTrue(!retiredTransport.isEmpty() && retiredTransport.stream().allMatch(referenceToOld -> referenceToOld.get() == null),
                        "Replaced transport owners collect while the current heated field remains live");
                helper.assertTrue(field.activeRouteCount() > 0 && field.read(receiver.position(), false, sample)
                                && sample.sampleTick() == 600 && sample.temperatureC() > 0,
                        "Check collection before closing the active reopened passage and source");
                FHMain.LOGGER.info("THERMAL_TILED_TRANSPORT_RETENTION,retired={},retained=0,coreStillLive=true", retiredTransport.size());
            }
            helper.assertTrue(budget.usedBytes() == 0, "Route geometry and source/core storage release their reservations");
        } finally {
            level.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            for (int x = origin.getX() >> 4; x <= (origin.getX() + 11) >> 4; x++) level.setChunkForced(x, origin.getZ() >> 4, false);
        }
        helper.succeed();
    }

    private static void rememberTransport(TiledThermalField field, Iterable<Long> sections, ArrayList<WeakReference<Object>> retired) {
        it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<?> tiles = ThermalLoadedWorldGameTests.read(field, "bySection");
        for (long section : sections) {
            Object transport = ThermalLoadedWorldGameTests.read(tiles.get(section), "transport");
            if (transport != null) retired.add(new WeakReference<>(transport));
        }
    }

    private static void step(GameTestHelper helper, TiledThermalField field, SpatialEnergyBanks banks, SpatialPowerTimeline powers, long tick) {
        helper.assertTrue(field.begin(powers.seal(tick)), "Begin the same physical input interval");
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!field.advance(System.nanoTime() + 1_000_000L)) helper.assertTrue(System.nanoTime() < deadline, "Passage step completes");
        helper.assertTrue(field.commit(), "Commit field and input together");
    }

    private static ThermalInputBatch input(long sequence, long tick, ResolvedGeometryBatch geometry) {
        return new ThermalInputBatch(1, sequence, tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES, geometry, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
    }

    private static int local(BlockPos position) {
        return (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
    }
}
