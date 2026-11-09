/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAirRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.LongPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_coordinator_overhead")
@PrefixGameTestTemplate(false)
public final class ThermalCoordinatorOverheadGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_coordinator_overhead", timeoutTicks = 200)
    public static void readyNumericalWorkSurvivesAnOverdueCoordinatorPrelude(GameTestHelper helper) throws ReflectiveOperationException {
        var origin = room(helper);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0)) {
            var runtime = (RegionalAirRuntime) simulation.material;
            simulation.engine.offerInput(new ThermalInputEnvelope(empty(++simulation.sequence, 40), new SpatialSourceTimeline.Window[0]));
            runtime.sealSpatialInput(40, Double.NaN);
            helper.assertTrue(runtime.hasReadyWork(), "The real room must have ready numerical work");
            helper.assertTrue(runtime.minimumSampleTick(40) < 40,
                    "The actual room must still be behind the offered boundary before the coordinator starts");
            long committed = runtime.commitRevision();
            var previous = ThermalDiagnostics.observer;
            int[] preludes = {0};
            ThermalDiagnostics.observer = new ThermalDiagnostics.Observer() {
                @Override public void engine(RegionalAirRuntime air, com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena cells,
                        com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger sources,
                        com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication queries,
                        ThermalMemoryBudget.Reservation backing, TopologyInbox topology, long tick) {
                    // Reproduce bookkeeping/preemption exceeding the old 2ms deadline.
                    long through = System.nanoTime() + 3_000_000L;
                    while (System.nanoTime() < through) Thread.onSpinWait();
                    preludes[0]++;
                }
            };
            try {
                for (int round = 0; round < 120 && runtime.minimumSampleTick(40) < 40; round++) simulation.engine.processWorkRound();
                helper.assertTrue(runtime.minimumSampleTick(40) == 40 && runtime.commitRevision() > committed,
                        "Ready numerical work must progress even when every coordinator prelude exceeds 2ms; preludes=" + preludes[0]);
            } finally { ThermalDiagnostics.observer = previous; }
            FHMain.LOGGER.info("THERMAL_COORDINATOR_PRELUDE_RESULT,sampleTick={},overduePrelude=true,preludes={}", runtime.minimumSampleTick(40), preludes[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_coordinator_overhead", timeoutTicks = 200)
    public static void incomingSourceMembershipUsesNoCheckpointObjects(GameTestHelper helper) {
        var origin = room(helper);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0);
                var timeline = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            WorkerPhysicalSourceBindings bindings = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceBindings");
            var fire = origin.offset(1, 0, 1);
            var descriptor = new SpatialSourceTimeline.Descriptor(811, 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), 1,
                    WorkerPhysicalSourceBindings.initialPorts(811, simulation.profiles.tuning().campfire()));
            long target = fire.above().asLong();
            LongPredicate owns = position -> position == target;
            helper.assertTrue(bindings.airPortTargets(descriptor, 0, owns), "The physical air port reaches the actual room");
            helper.assertTrue(bindings.checkpointTarget(descriptor, 0).target().position() == target,
                    "Readiness and immutable save destination agree");
            helper.assertTrue(!bindings.airPortTargets(descriptor, 1, owns), "Declared radiation loss is not a thermal target");
            var missing = new SpatialSourceTimeline.Descriptor(812, 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX() + 64, fire.getY(), fire.getZ(), 1, descriptor.ports());
            helper.assertTrue(!bindings.airPortTargets(missing, 0, owns), "Unavailable geometry must not reuse the previous scratch destination");
            helper.assertTrue(timeline.register(descriptor, 20, true, 0), "A real source owns a producer window");
            simulation.engine.offerInput(new ThermalInputEnvelope(empty(++simulation.sequence, 0), timeline.seal(0)));
            SpatialSourceIngress ingress = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceIngress");
            var sections = new LongOpenHashSet(new long[] {SectionPos.asLong(fire)});
            helper.assertTrue(ingress.pendingFor(sections, ignored -> false, owns) == 0,
                    "An incoming source holds its actual receiving region at the registration boundary");
            helper.assertTrue(ingress.pendingFor(sections, ignored -> true, owns) == Long.MAX_VALUE,
                    "A source already indexed by the region uses its ordinary source boundary");
            helper.assertTrue(ingress.pendingFor(sections, ignored -> false, ignored -> false) == Long.MAX_VALUE,
                    "Unrelated regions do not acquire a source hold");
            for (int index = 0; index < 30_000; index++) bindings.airPortTargets(descriptor, 0, owns);
            var allocations = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            long thread = Thread.currentThread().getId(), before = allocations.getThreadAllocatedBytes(thread);
            int matched = 0;
            for (int index = 0; index < 30_000; index++) if (bindings.airPortTargets(descriptor, 0, owns)) matched++;
            long bytes = allocations.getThreadAllocatedBytes(thread) - before;
            helper.assertTrue(matched == 30_000 && bytes < 65_536,
                    "Readiness must not allocate destination records per physical port; allocated=" + bytes);
            FHMain.LOGGER.info("THERMAL_SOURCE_MEMBERSHIP_RESULT,queries={},allocatedBytes={},incomingHold=0", matched, bytes);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_coordinator_overhead", timeoutTicks = 200)
    public static void waitingGeometryOwnsOnlyBoundaryUntilSourcesAreFrozen(GameTestHelper helper) throws ReflectiveOperationException {
        var origin = room(helper);
        room(helper, origin.east(16));
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 0, 0, 0, 0)) {
            var handles = new ThermalPageHandle[2];
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            for (int index = 0; index < handles.length; index++) {
                var at = origin.east(index * 16);
                var handle = handles[index] = new ThermalPageHandle(SectionPos.asLong(at), 1);
                long mask = 1L << ((at.getX() & 15) >>> 2 | ((at.getZ() & 15) >>> 2) << 2 | ((at.getY() & 15) >>> 2) << 4);
                var admission = new ThermalInputBatch.PageAdmission(handle, 0, mask, mask,
                        simulation.capture.captureBricks(handle.sectionKey(), null, simulation.capture.unresolvedPage(), mask), 0, sky, null);
                var batch = new ThermalInputBatch(1, ++simulation.sequence, index * 20,
                        new ThermalInputBatch.PageAdmission[] {admission}, ThermalInputBatch.NO_RETIREMENTS,
                        ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                        ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
                simulation.engine.offerInput(new ThermalInputEnvelope(batch, new SpatialSourceTimeline.Window[0]));
            }
            TopologyInbox inbox = ThermalLoadedWorldGameTests.read(simulation.engine, "topologyInputs");
            var lane = simulation.engine.getClass().getDeclaredField("nextWorkLane"); lane.setAccessible(true);
            for (int round = 0; round < 20 && inbox.pendingSectionCount() < 2; round++) {
                lane.setInt(simulation.engine, 0); simulation.engine.processWorkRound();
            }
            helper.assertTrue(inbox.pendingSectionCount() == 2 && inbox.nextPendingSection() == handles[0].sectionKey(),
                    "Both real admissions are queued before visiting the later one first");
            var start = simulation.engine.getClass().getDeclaredMethod("startGeometryWork", long.class); start.setAccessible(true);
            start.invoke(simulation.engine, System.nanoTime() + 1_000_000_000L);
            Long2ObjectOpenHashMap<?> candidates = ThermalLoadedWorldGameTests.read(simulation.engine, "pendingBoundaries");
            var candidate = candidates.get(handles[1].sectionKey());
            helper.assertTrue(candidate != null, "The later geometry waits on the actual earlier input");
            ThermalMemoryBudget.Reservation credit = ThermalLoadedWorldGameTests.read(candidate, "reservation");
            RegionalContinuationBoundary boundary = ThermalLoadedWorldGameTests.read(candidate, "boundary");
            helper.assertTrue(credit.bytes() == 4096L + 64L * boundary.sectionCount(),
                    "A waiting boundary must not prepay a future frozen source snapshot; bytes=" + credit.bytes());
            start.invoke(simulation.engine, System.nanoTime() + 1_000_000_000L);
            ArrayDeque<?> works = ThermalLoadedWorldGameTests.read(simulation.engine, "geometryWorks");
            helper.assertTrue(!works.isEmpty(), "The earlier geometry can freeze its real source snapshot");
            var work = works.peekFirst();
            ThermalMemoryBudget.Reservation selectedCredit = ThermalLoadedWorldGameTests.read(work, "boundaryReservation");
            WorkerPhysicalSourceBindings.FrozenSources frozen = ThermalLoadedWorldGameTests.read(work, "emitters");
            RegionalContinuationBoundary selectedBoundary = ThermalLoadedWorldGameTests.read(work, "boundary");
            try (var meter = new AirBackingMeter()) {
                frozen.recordBacking(meter);
                helper.assertTrue(selectedCredit.bytes() >= 4096L + 64L * selectedBoundary.sectionCount() + meter.bytes(),
                        "The selected work must cover its actual source snapshot before construction");
            }
            simulation.tick = 20;
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(handles[0].currentPublication() != null && handles[1].currentPublication() != null,
                    "Returning unused source credit must preserve actual geometry publication");
            FHMain.LOGGER.info("THERMAL_BOUNDARY_CREDIT_RESULT,waitingMetadataOnly=true,actualFreezeCovered=true,bothPagesPublished=true");
        }
        helper.succeed();
    }

    private static ThermalInputBatch empty(long sequence, long tick) {
        return new ThermalInputBatch(1, sequence, tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
    }
    private static BlockPos room(GameTestHelper helper) {
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        var anchor = helper.absolutePos(new BlockPos(8, 8, 8));
        var origin = new BlockPos(anchor.getX() & ~3, anchor.getY() & ~3, anchor.getZ() & ~3);
        room(helper, origin); return origin;
    }
    private static void room(GameTestHelper helper, BlockPos origin) {
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z),
                    (x == 0 || y == 0 || z == 0 || x == 3 || y == 3 || z == 3 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
    }
}
