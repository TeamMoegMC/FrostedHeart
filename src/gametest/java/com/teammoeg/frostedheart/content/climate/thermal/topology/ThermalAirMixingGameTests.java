/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.*;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.*;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.*;
import com.teammoeg.frostedheart.content.climate.thermal.solver.*;
import com.teammoeg.frostedheart.content.climate.thermal.source.*;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.*;

import java.lang.reflect.Field;
import java.util.*;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalAirMixingGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void oneNodePairKeepsDistinctHorizontalAndVerticalContacts(GameTestHelper helper) {
        var table = ThermalSignatureTable.builder();
        int air = table.intern(new ResolvedThermalSignature(100, 0));
        int restrictedAir = table.intern(new ResolvedThermalSignature(50, 0, -1, 0, true));
        var signatures = table.build();
        var profiles = MinecraftThermalProfiles.prepare();
        var tuning = profiles.tuning();
        var arena = new ThermalCellArena(256);
        var query = QueryPublication.tryCreate(new ThermalMemoryBudget(16L << 20).createDimensionBudget(16L << 20), 256, 64);
        try (var engine = new ThermalDimensionEngine(1, 0, arena, signatures, profiles.materials(),
                new ThermalTopologyParameters(tuning.airHeatCapacityJPerBlockK(), 0, tuning.airMixingWPerBlockK(), new BuoyancyConductance.Parameters(.25, 4, 10), 1024, 8),
                new FarFieldSettings(tuning.farFieldConductanceWPerK(), 32, 16), tuning.campfire(),
                new ThermalDimensionLimits(64, 128, 256, 1024, 1024, 4096, 4096, 200, 1e-6), query)) {
            int[] ids = new int[64]; Arrays.fill(ids, air); ids[1 | 1 << 2 | 1 << 4] = restrictedAir;
            var cut = new PageSignatures.Builder(signatures).reset(PageSignatures.unresolved(signatures)).setBrick(0, ids).buildBricks();
            var page = new ThermalPageHandle(SectionPos.asLong(0, 4, 0), 1);
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 0, cut, 0, sky, null);
            var result = engine.process(new ThermalInputBatch(1, 1, 0, new ThermalInputBatch.PageAdmission[]{admission}, ThermalInputBatch.NO_RETIREMENTS,
                    ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalSourceBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN));
            helper.assertTrue(result.status() == ThermalCompletion.Status.COMPLETED, "mixed-Air admission");
            ThermalSolver solver = read(engine, "solver");
            var pairs = solver.fragment(page.currentPublication().workerPageSlot() * 64).airPairs();
            helper.assertTrue(arena.liveCellCount() == 2 && pairs.size() == 3, "six faces of one node pair form three direction groups");
            int directions = 0;
            for (int i = 0; i < pairs.size(); i++) directions |= 1 << pairs.direction(i);
            helper.assertTrue(directions == 7, "both vertical orientations survive alongside horizontal contacts");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 160)
    public static void stableHeatingConservesEnergyWithoutTopologyWork(GameTestHelper helper) {
        Geometry floor = (x, y, z) -> y <= 65 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        try (Fixture f = new Fixture(helper, new BlockPos(0, 64, 0), 32, 8, 32, floor, 16384, false)) {
            double initial = f.energy();
            var events = new ThermalSourceBatch.Builder(0);
            int source = 0;
            for (int x = 5; x <= 29; x += 8) for (int z = 5; z <= 29; z += 8) f.register(events, ++source, 1, new BlockPos(x, 66, z));
            f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            long version = f.solver.structuralVersion();
            for (int step = 0; step < 60; step++) { f.tick += 20; f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY); }
            near(helper, initial + source * 6400.0 * 60, f.energy(), "Air/material transfer conserves delivered source energy");
            ThermalSourceLedger ledger = read(f.engine, "sources");
            near(helper, source * 1600.0 * 60, ledger.energyBalance().declaredLossJ(), "independent radiation share remains unchanged");
            helper.assertTrue(f.solver.structuralVersion() == version && f.bindings.mixingChanges().isEmpty(), "steady sources do not rebuild fragments");
            helper.assertTrue(f.arena.temperatureC(f.slot(new BlockPos(5, 67, 5)), 0) > -40, "source heats the physical Air node");
            for (int i = 0; i < 1024; i++) f.solver.step(1, (i & 1) == 0);
            var counter = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
            counter.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().getId();
            long[] times = new long[128];
            long allocated = counter.getThreadAllocatedBytes(thread);
            for (int i = 0; i < times.length; i++) { long start = System.nanoTime(); f.solver.step(1, (i & 1) == 0); times[i] = System.nanoTime() - start; }
            allocated = counter.getThreadAllocatedBytes(thread) - allocated;
            Arrays.sort(times);
            FHMain.LOGGER.info("Air mixing steady solver: sources={}, cells={}, p50_us={}, p95_us={}, allocated_bytes={}", source, f.arena.liveCellCount(), times[64] / 1000.0, times[121] / 1000.0, allocated);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void exactFaceAreaDoesNotMultiplyOverlappingSources(GameTestHelper helper) {
        AirMixingRegion region = new AirMixingRegion();
        region.reset(-4, -4, -4);
        near(helper, 16, region.brickFaceArea(0, -4, -4, -4), "no source");
        region.include(-4, -2, -3);
        near(helper, 43, region.brickFaceArea(0, -4, -4, -4), "nine of sixteen unit faces lie inside the sphere");
        region.include(-4, -2, -3);
        near(helper, 43, region.brickFaceArea(0, -4, -4, -4), "overlap is a union");
        near(helper, 1, region.faceArea(0, 12, -2, -3), "far face is unchanged");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void actualContactDirectionSurvivesCentroidShiftAndEndpointOrder(GameTestHelper helper) {
        Geometry shiftedFloor = (x, y, z) -> x < 4 && y == 64 && z == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        try (Fixture f = new Fixture(helper, new BlockPos(0, 64, 0), 8, 4, 4, shiftedFloor, 256, false)) {
            int left = f.slot(new BlockPos(2, 66, 2)), right = f.slot(new BlockPos(6, 66, 2));
            helper.assertTrue(f.arena.center(left, 1) != f.arena.center(right, 1), "centroids deliberately differ");
            helper.assertTrue(f.direction(left, right) == ThermalFragment.AirPairs.HORIZONTAL, "horizontal contact must stay horizontal");
            var result = new BuoyancyConductance.MutableResult();
            BuoyancyConductance.evaluateInto(10, 100, -100, f.direction(left, right), new BuoyancyConductance.Parameters(.25, 4, 10), result);
            near(helper, 10, result.conductanceWPerK(), "horizontal face has no buoyancy multiplier");
        }
        try (Fixture f = new Fixture(helper, new BlockPos(0, 76, 0), 4, 8, 4, AIR, 256, true)) {
            int lower = f.slot(new BlockPos(1, 77, 1)), upper = f.slot(new BlockPos(1, 81, 1));
            helper.assertTrue(lower > upper, "upper Page is allocated first");
            helper.assertTrue(f.direction(lower, upper) == ThermalFragment.AirPairs.SECOND_BELOW, "direction follows ordered IDs");
            var result = new BuoyancyConductance.MutableResult();
            BuoyancyConductance.evaluateInto(10, 0, 20, f.direction(lower, upper), new BuoyancyConductance.Parameters(.25, 4, 10), result);
            near(helper, 30, result.conductanceWPerK(), "hot lower endpoint enhances vertical exchange");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sourceLifecycleChangesConductanceWithoutReplacingCells(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, new BlockPos(-8, 64, -4), 24, 4, 4, AIR, 256, false)) {
            BlockPos left = new BlockPos(1, 66, -3), right = new BlockPos(5, 66, -3);
            int a = f.slot(left), b = f.slot(right), count = f.arena.liveCellCount();
            double base = f.conductance(a, b), energy = f.energy();
            var events = new ThermalSourceBatch.Builder(0);
            f.register(events, 1, 1, new BlockPos(0, 65, -3));
            f.register(events, 2, 1, new BlockPos(0, 65, -3));
            f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            near(helper, base * 43 / 16, f.conductance(a, b), "exact partial full-Air face and overlapping sources");
            helper.assertTrue(count == f.arena.liveCellCount() && a == f.slot(left) && b == f.slot(right), "source-only change retains arena slots");
            near(helper, energy, f.energy(), "coefficient update does not alter H");
            events.addUnload(1, 1, 0); f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            near(helper, base * 43 / 16, f.conductance(a, b), "other source still owns enhancement");
            events.addEnabledChange(2, false, 0); f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            near(helper, base, f.conductance(a, b), "disabled source restores original G");
            events.addEnabledChange(2, true, 0); f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            long version = f.solver.structuralVersion();
            events.addPowerChange(2, 4000, 0); f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            helper.assertTrue(version == f.solver.structuralVersion(), "positive power adjustment does not rebuild geometry");
            events.addPowerChange(2, 0, 0); f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            near(helper, base, f.conductance(a, b), "zero power removes local mixing");
            events.addUnload(2, 1, 0);
            f.register(events, 2, 2, new BlockPos(4, 65, -3));
            f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            near(helper, base * 4, f.conductance(a, b), "moved source updates both old and new ranges");
            helper.assertTrue(f.bindings.mixingChanges().isEmpty(), "successful commit consumes pending work");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sourceMixingChangeSurvivesRefusedGeometryBudget(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, new BlockPos(-8, 64, -4), 24, 4, 4, AIR, 7, false)) {
            int a = f.slot(new BlockPos(1, 66, -3)), b = f.slot(new BlockPos(5, 66, -3));
            double base = f.conductance(a, b);
            var events = new ThermalSourceBatch.Builder(0);
            f.register(events, 1, 1, new BlockPos(0, 65, -3)); f.cut(events.buildAndReset(), ResolvedGeometryBatch.EMPTY);
            events.addEnabledChange(1, false, 0);
            var refused = f.process(events.buildAndReset(), f.change(new BlockPos(12, 64, -4), Blocks.STONE.defaultBlockState()));
            helper.assertTrue(refused.status() == ThermalCompletion.Status.WORK_LIMITED, "material replacement cannot fit staging budget");
            helper.assertTrue(!f.bindings.mixingChanges().isEmpty(), "pending source change survives refusal");
            f.cut(ThermalSourceBatch.EMPTY, f.change(new BlockPos(12, 64, -4), Blocks.AIR.defaultBlockState()));
            near(helper, base, f.conductance(a, b), "later commit restores non-source G");
            helper.assertTrue(f.bindings.mixingChanges().isEmpty(), "retry completes pending work");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void coldMaterialNeighborKeepsItsIncomingRequest(GameTestHelper helper) {
        Geometry wall = (x, y, z) -> x >= 16 || y <= 65 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        try (Fixture f = new Fixture(helper, new BlockPos(12, 64, 0), 8, 4, 4, wall, 1024, false)) {
            int air = f.slot(new BlockPos(14, 66, 1));
            double initial = f.arena.enthalpyJ(air);
            f.arena.addEnthalpyJ(air, 2 * f.arena.capacityJPerK(air));
            long target = SectionPos.asLong(1, 4, 0);
            for (int i = 0; i < 4; i++) {
                f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
                Long2LongOpenHashMap desired = read(f.pages, "desiredBySection");
                helper.assertTrue(desired.get(target) != 0, "resident cold neighbor remains requested");
            }
            f.arena.setEnthalpyJ(air, initial); f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
            Long2LongOpenHashMap desired = read(f.pages, "desiredBySection");
            helper.assertTrue(desired.get(target) == 0, "cold owner no longer pins its neighbor");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void activityThresholdsKeepTheCompletedHotMask(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, new BlockPos(0, 64, 0), 4, 4, 4, AIR, 256, false)) {
            int slot = f.slot(new BlockPos(1, 65, 1)), page = f.arena.pageSlot(slot);
            double initial = f.arena.enthalpyJ(slot), capacity = f.arena.capacityJPerK(slot);
            double[] changes = {.75, 1, .75, .5, .49, -1, -.75, -.49};
            boolean[] expected = {false, true, true, true, false, true, true, false};
            for (int i = 0; i < changes.length; i++) {
                f.arena.setEnthalpyJ(slot, initial + capacity * changes[i]);
                f.cut(ThermalSourceBatch.EMPTY, ResolvedGeometryBatch.EMPTY);
                helper.assertTrue((f.pages.hotMaskScratch().hotMask(page) != 0) == expected[i], "1/.5 hysteresis and completed-cut visibility at " + changes[i]);
            }
        }
        helper.succeed();
    }

    interface Geometry { BlockState at(int x, int y, int z); }
    private static final Geometry AIR = (x, y, z) -> Blocks.AIR.defaultBlockState();

    static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final MinecraftThermalProfiles.Snapshot profiles = MinecraftThermalProfiles.prepare();
        final ThermalCellArena arena = new ThermalCellArena(256);
        final QueryPublication query = QueryPublication.tryCreate(new ThermalMemoryBudget(16L << 20).createDimensionBudget(16L << 20), 256, 64);
        final Map<Long, ThermalPageHandle> handles = new LinkedHashMap<>();
        final ThermalDimensionEngine engine;
        final ThermalSolver solver;
        final WorkerPageStore pages;
        final WorkerPhysicalSourceBindings bindings;
        long sequence, tick;

        Fixture(GameTestHelper helper, BlockPos origin, int sx, int sy, int sz, Geometry geometry, int limit, boolean reversePages) {
            this.helper = helper;
            var tuning = profiles.tuning();
            engine = new ThermalDimensionEngine(1, 0, arena, profiles.signatures(), profiles.materials(),
                    new ThermalTopologyParameters(tuning.airHeatCapacityJPerBlockK(), 0, tuning.airMixingWPerBlockK(), new BuoyancyConductance.Parameters(.25, 4, 10), 1024, 8),
                    new FarFieldSettings(tuning.farFieldConductanceWPerK(), 32, 16), tuning.campfire(),
                    new ThermalDimensionLimits(64, 128, 256, limit, 16384, 131072, 65536, 200, 1e-6), query);
            solver = read(engine, "solver"); pages = read(engine, "pages"); bindings = read(engine, "sourceBindings");
            var masks = new LinkedHashMap<Long, Long>();
            for (int x = 0; x < sx; x += 4) for (int y = 0; y < sy; y += 4) for (int z = 0; z < sz; z += 4) {
                BlockPos p = origin.offset(x, y, z); masks.merge(SectionPos.asLong(p), 1L << brick(p), (a, b) -> a | b);
            }
            var admissions = new ArrayList<ThermalInputBatch.PageAdmission>();
            for (var entry : masks.entrySet()) {
                long key = entry.getKey(); var handle = new ThermalPageHandle(key, 1); handles.put(key, handle);
                var signatures = new PageSignatures.Builder(profiles.signatures()).reset(PageSignatures.unresolved(profiles.signatures()));
                for (int b = 0; b < 64; b++) {
                    int[] ids = new int[64];
                    for (int p = 0; p < 64; p++) ids[p] = profiles.states().signatureId(geometry.at(
                            SectionPos.x(key) * 16 + (b & 3) * 4 + (p & 3), SectionPos.y(key) * 16 + (b >>> 4) * 4 + (p >>> 4), SectionPos.z(key) * 16 + (b >>> 2 & 3) * 4 + (p >>> 2 & 3)));
                    signatures.setBrick(b, ids);
                }
                byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
                admissions.add(new ThermalInputBatch.PageAdmission(handle, 0, entry.getValue(), 0, signatures.buildBricks(), -40, sky, null));
            }
            if (reversePages) Collections.reverse(admissions);
            var result = engine.process(new ThermalInputBatch(1, ++sequence, 0, admissions.toArray(ThermalInputBatch.PageAdmission[]::new), ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                    ResolvedGeometryBatch.EMPTY, ThermalSourceBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN));
            helper.assertTrue(result.status() == ThermalCompletion.Status.COMPLETED, "fixture admission " + result.failure());
        }
        void register(ThermalSourceBatch.Builder batch, long id, int generation, BlockPos anchor) {
            batch.addRegister(id, generation, ThermalSourceMode.POWER_SOURCE, 8000, true, 0, anchor.getX(), anchor.getY(), anchor.getZ(), profiles.tuning().campfire().profileId(),
                    WorkerPhysicalSourceBindings.initialPorts(id, profiles.tuning().campfire()));
        }
        ThermalCompletion process(ThermalSourceBatch events, ResolvedGeometryBatch geometry) {
            return engine.process(new ThermalInputBatch(1, ++sequence, tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                    geometry, events, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN));
        }
        void cut(ThermalSourceBatch events, ResolvedGeometryBatch geometry) {
            var result = process(events, geometry); helper.assertTrue(result.status() == ThermalCompletion.Status.COMPLETED, "cut " + result.failure());
        }
        ResolvedGeometryBatch change(BlockPos p, BlockState state) {
            var handle = handles.get(SectionPos.asLong(p)); var geometry = new ResolvedGeometryBatch.Builder();
            geometry.addResolvedCenter(handle, handle.beginGeometryMutation(), (p.getX() & 15) | (p.getZ() & 15) << 4 | (p.getY() & 15) << 8, profiles.states().signatureId(state));
            return geometry.buildAndReset();
        }
        int slot(BlockPos p) {
            var b = handles.get(SectionPos.asLong(p)).currentPublication().brick(brick(p));
            int local = (p.getX() & 3) | (p.getZ() & 3) << 2 | (p.getY() & 3) << 4;
            return b.firstSlot() + (b.blockLayout() == null ? 0 : b.blockLayout().nodeAt(local));
        }
        double conductance(int a, int b) { double total = 0; for (var handle : handles.values()) for (int i = 0; i < 64; i++) {
            var pairs = solver.fragment(handle.lastPublication().workerPageSlot() * 64 + i).airPairs();
            for (int n = 0; n < pairs.size(); n++) if (pairs.first(n) == Math.min(a, b) && pairs.second(n) == Math.max(a, b)) total += pairs.conductance(n);
        } return total; }
        byte direction(int a, int b) { for (var handle : handles.values()) for (int i = 0; i < 64; i++) {
            var pairs = solver.fragment(handle.lastPublication().workerPageSlot() * 64 + i).airPairs();
            for (int n = 0; n < pairs.size(); n++) if (pairs.first(n) == Math.min(a, b) && pairs.second(n) == Math.max(a, b)) return pairs.direction(n);
        } throw new AssertionError("missing pair"); }
        double energy() { double result = 0; for (int s = arena.nextLiveSlot(0); s >= 0; s = arena.nextLiveSlot(s + 1)) result += arena.enthalpyJ(s); return result; }
        @Override public void close() { engine.close(); }
    }
    private static int brick(BlockPos p) { return (p.getX() & 15) >>> 2 | ((p.getZ() & 15) >>> 2) << 2 | ((p.getY() & 15) >>> 2) << 4; }
    private static void near(GameTestHelper h, double expected, double actual, String message) { h.assertTrue(Math.abs(expected - actual) <= 1e-8 * Math.max(1, Math.abs(expected)), message + ": " + actual + " expected " + expected); }
    @SuppressWarnings("unchecked") private static <T> T read(Object o, String name) { try { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return (T) f.get(o); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); } }
}
