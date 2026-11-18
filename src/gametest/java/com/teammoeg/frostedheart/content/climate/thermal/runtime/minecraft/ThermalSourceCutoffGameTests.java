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
            var descriptor = new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile));
            helper.assertTrue(producer.register(descriptor, 20, true, 0), "Register a real source with the ordinary physical outlet resolver");
            submit(helper, simulation, 0, producer.seal(0));
            submit(helper, simulation, 20, producer.seal(20));
            WorkerPhysicalSourceBindings bindings = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceBindings");
            int[] clockReads = {0};
            long limit = bindings.bindingLimit(descriptor, key -> { clockReads[0]++; return 20; });
            helper.assertTrue(limit == 20 && clockReads[0] == 1, "An anchor and outlets in the same Section require one cutoff lookup");
            // An unregistered source at a Section edge still has its declared port cutoff.
            var edge = new BlockPos(fire.getX(), (fire.getY() | 15), fire.getZ());
            var atEdge = new SpatialSourceTimeline.Descriptor(edge.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    edge.getX(), edge.getY(), edge.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(edge.asLong(), profile));
            helper.assertTrue(bindings.bindingLimit(atEdge, key -> key == atEdge.section() ? 40 : 20) == 20,
                    "A port in the next Section retains its earlier limit");
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
            var domain = it.unimi.dsi.fastutil.longs.LongSets.singleton(net.minecraft.core.SectionPos.asLong(fire));
            helper.assertTrue(ingress.limitNumericalThrough(domain, section -> true, 20, 60) == 40
                            && ingress.limitNumericalThrough(domain, section -> true, 45, 60) == 45
                            && ingress.limitNumericalThrough(domain, section -> false, 20, 60) == 60,
                    "A domain cuts at the pending event, clamps late input, and ignores uncaptured sections");
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
            compareFrozenSourceCutoff(helper, simulation, origin);
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

    /** Same real windows and physical port resolver for the unrestricted and interval-bounded queries. */
    private static void compareFrozenSourceCutoff(GameTestHelper helper,
            ThermalMaterialBehaviorGameTests.Simulation simulation, BlockPos origin) {
        var profile = simulation.profiles.tuning().campfire();
        WorkerPhysicalSourceBindings bindings = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceBindings");
        long chunk = net.minecraft.world.level.ChunkPos.asLong(origin.getX() >> 4, origin.getZ() >> 4);
        var domain = it.unimi.dsi.fastutil.longs.LongSets.singleton(net.minecraft.core.SectionPos.asLong(origin));
        int[] checks = {0};
        long[] snapshotRevision = {0};
        try (var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 64);
                var ingress = new SpatialSourceIngress(ThermalLoadedWorldGameTests.read(simulation.engine, "sources"),
                        bindings, simulation.engine, simulation.arena, ThermalLoadedWorldGameTests.read(simulation.engine, "airLoads"),
                        d -> Long.MAX_VALUE, d -> { checks[0]++; return bindings.airPortsTouchChunk(d, chunk); },
                        () -> snapshotRevision[0], (window, positions) -> {})) {
            for (int n = 0; n < 64; n++) {
                var pos = origin.offset(n % 8, 0, n / 8);
                var desc = new SpatialSourceTimeline.Descriptor(pos.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                        pos.getX(), pos.getY(), pos.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(pos.asLong(), profile));
                helper.assertTrue(producer.register(desc, 20, true, 40), "Register the source at the interval endpoint");
            }
            var windows = producer.seal(40);
            helper.assertTrue(windows.length == 64, "Each pending source has a real prepaid window");
            for (int n = 0; n < windows.length; n++) ingress.enqueue(windows[n], 1, n);
            // An earlier frozen input still belongs to its checkpoint and must not restrict a later endpoint.
            helper.assertTrue(ingress.limitNumericalThrough(domain, section -> true, 20, 60) == 60 && checks[0] == 64,
                    "All in-range frozen sources are checked and excluded");
            var cpu = java.lang.management.ManagementFactory.getThreadMXBean();
            boolean timed = cpu.isCurrentThreadCpuTimeSupported() && cpu.isThreadCpuTimeEnabled();
            long[] ns = new long[2]; int[] attempts = new int[2];
            // Alternate order inside one server test and one JVM. Each round changes the actual snapshot revision.
            for (int pass = 0; pass < 6; pass++) for (int arm = 0; arm < 2; arm++) {
                boolean bounded = ((pass + arm) & 1) != 0;
                int count = pass < 2 ? 2048 : 32768, sum = 0; checks[0] = 0;
                long start = timed ? cpu.getCurrentThreadCpuTime() : 0;
                for (int q = 0; q < count; q++) {
                    snapshotRevision[0]++;
                    sum += bounded ? ingress.limitNumericalThrough(domain, section -> true, 20, 40)
                            : Math.min(40, ingress.firstPendingTick(domain, section -> true));
                }
                long elapsed = timed ? cpu.getCurrentThreadCpuTime() - start : -1;
                helper.assertTrue(sum == 40 * count && checks[0] == (bounded ? 0 : count * 64),
                        "Both paths preserve the cutoff; only an earlier input requires a physical freeze lookup");
                if (pass >= 2) { ns[bounded ? 1 : 0] += elapsed; attempts[bounded ? 1 : 0] += checks[0]; }
            }
            FHMain.LOGGER.info("THERMAL_SCOPED_SOURCE_CPU,queriesPerArm=131072,sources=64,unboundedLookups={},boundedLookups={},unboundedCpuNs={},boundedCpuNs={}",
                    attempts[0], attempts[1], timed ? ns[0] : -1, timed ? ns[1] : -1);
        }
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
