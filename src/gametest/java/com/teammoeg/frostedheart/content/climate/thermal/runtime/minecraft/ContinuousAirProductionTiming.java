/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Observe actual Forge source scenes through ordinary queries, without writing heat state or blocks. */
final class ContinuousAirProductionTiming {
    private ContinuousAirProductionTiming() {}
    private static final boolean PROFILE = Boolean.parseBoolean(System.getenv("FH_CONTINUOUS_AIR_PROFILE"));

    /** One native sequence, not thousands of future map entries. Actions retain their tick offsets. */
    static void eachTick(GameTestHelper helper, int endTick, java.util.function.IntConsumer observer) {
        long startedTick = helper.getLevel().getGameTime();
        // Native sequences run after this tick's scheduled actions. The old map
        // did not order same-tick actions; final cleanup still starts one tick later.
        helper.startSequence().thenExecuteFor(endTick + 1, () -> {
            int elapsed = Math.toIntExact(helper.getLevel().getGameTime() - startedTick);
            if (elapsed >= 1 && elapsed <= endTick) observer.accept(elapsed);
        });
    }

    /** Platform CPU time is retained for diagnosis; Windows bursty-work totals can severely undercount. */
    static long[] resources() {
        com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool.shared();
        var bean = java.lang.management.ManagementFactory.getThreadMXBean();
        if (bean.isThreadCpuTimeSupported() && !bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
        var allocation = bean instanceof com.sun.management.ThreadMXBean extended ? extended : null;
        if (allocation != null && allocation.isThreadAllocatedMemorySupported() && !allocation.isThreadAllocatedMemoryEnabled())
            allocation.setThreadAllocatedMemoryEnabled(true);
        long workerCpu = 0, workerBytes = 0;
        for (long id : bean.getAllThreadIds()) {
            var info = bean.getThreadInfo(id);
            if (info == null || !info.getThreadName().startsWith("frosted-heart-thermal")) continue;
            if (bean.isThreadCpuTimeSupported()) workerCpu += Math.max(0, bean.getThreadCpuTime(id));
            if (allocation != null && allocation.isThreadAllocatedMemorySupported())
                workerBytes += Math.max(0, allocation.getThreadAllocatedBytes(id));
        }
        long mainCpu = bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0;
        long mainBytes = bean instanceof com.sun.management.ThreadMXBean extended && extended.isThreadAllocatedMemorySupported()
                ? extended.getThreadAllocatedBytes(Thread.currentThread().getId()) : 0;
        long gcCount = 0, gcMillis = 0;
        for (var collector : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, collector.getCollectionCount()); gcMillis += Math.max(0, collector.getCollectionTime());
        }
        return new long[] {workerCpu, workerBytes, mainCpu, mainBytes, gcCount, gcMillis,
                java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()};
    }
    private static final boolean OBSERVE_TEMPERATURE_ENVELOPE = "true".equalsIgnoreCase(
            System.getenv("FH_CONTINUOUS_AIR_TEMPERATURE_OBSERVATION"));

    /** Real published Air only: near every fire, sub-block positions and room height/distance. */
    static void observeTemperatureEnvelope(GameTestHelper helper, BlockPos[] fires, BlockPos[] probes, int endTick) {
        if (!OBSERVE_TEMPERATURE_ENVELOPE) return;
        var points = new java.util.ArrayList<double[]>();
        for (BlockPos fire : fires) for (double height : new double[] {1.125, 1.875})
            points.add(new double[] {fire.getX() + .5, fire.getY() + height, fire.getZ() + .5});
        for (BlockPos probe : probes) {
            for (int distance = -2; distance <= 2; distance++)
                for (int height = 0; height <= 2; height++)
                    points.add(new double[] {probe.getX() + distance + .5, probe.getY() + height + .3125, probe.getZ() + .5});
            for (double dx : new double[] {.125, .875}) for (double dz : new double[] {.125, .875})
                points.add(new double[] {probe.getX() + dx, probe.getY() + .5, probe.getZ() + dz});
        }
        var sample = new com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication.MutableSample();
        for (int delay = 40; delay <= endTick; delay += 40) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                var publication = MinecraftThermalInput.queryPublicationForObservation(helper.getLevel());
                for (int index = 0; index < points.size(); index++) {
                    double[] point = points.get(index);
                    BlockPos block = BlockPos.containing(point[0], point[1], point[2]);
                    boolean available = publication != null && publication.tryReadAir(point[0], point[1], point[2], sample);
                    FHMain.LOGGER.info("THERMAL_TEMPERATURE_ENVELOPE,elapsedTicks={},point={},x={},y={},z={},sampleTick={},naturalC={},airC={},available={},current={}",
                            elapsed, index, point[0], point[1], point[2], available ? sample.sampleTick() : -1,
                            WorldTemperature.naturalAir(helper.getLevel(), block), available ? sample.temperatureC() : Double.NaN,
                            available, MinecraftThermalInput.describeAirRead(helper.getLevel(), block).startsWith("geometry_current"));
                }
            });
        }
    }

    static boolean detailedBasis(int terms) {
        return terms >= 8;
    }

    static boolean sourceField(ThermalEnvironmentSample sample) {
        return sample.airAvailable() && sample.airSampleTick() >= 0 && detailedBasis(sample.airBasisTerms());
    }

    static void observe(GameTestHelper helper, String scene, BlockPos[] probes, int endTick) {
        var level = helper.getLevel();
        ContinuousAirGameTestPacing.beginMeasurement();
        long startedNanos = System.nanoTime();
        long startedBarrierNanos = ContinuousAirGameTestPacing.barrierWaitNanos();
        long[] startedResources = resources();
        if (ThermalNativeCpuObservation.ENABLED) ThermalNativeCpuObservation.begin();
        long[] mainTickTimes = new long[endTick];
        long[] serverProcessingTimes = new long[endTick];
        long[] maxServerProcessing = new long[2];
        long[] heapPeak = {startedResources[6]};
        FHMain.LOGGER.info("THERMAL_TEST_CLOCK_START,scene={},mode={},gameTick={},plannedTicks={}",
                scene, ContinuousAirGameTestPacing.clockMode(), level.getGameTime(), endTick);
        var heap = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        FHMain.LOGGER.info("THERMAL_TEST_JVM,scene={},javaVersion={},initialHeapBytes={},maxHeapBytes={},availableProcessors={}",
                scene, System.getProperty("java.version"), heap.getInit(), heap.getMax(), Runtime.getRuntime().availableProcessors());
        FHMain.LOGGER.info("THERMAL_TEST_CPU_COUNTER,scene={},kind=ThreadMXBean,platform={},interpretation={},nativeCyclesEnabled={}",
                scene, System.getProperty("os.name"), System.getProperty("os.name").startsWith("Windows")
                        ? "bursty_work_accounting_unreliable" : "platform_cpu_time", ThermalNativeCpuObservation.ENABLED);
        boolean[] firstResponse = {false};
        boolean[] firstLiveRead = {false};
        var air = new ThermalEnvironmentSample();
        eachTick(helper, endTick, elapsed -> {
                if (ThermalNativeCpuObservation.ENABLED && elapsed % 200 == 0)
                    ThermalNativeCpuObservation.sample(scene, elapsed, elapsed == endTick);
                {
                    mainTickTimes[elapsed - 1] = MinecraftThermalInput.observedMainTickNanos(level);
                    // The preceding complete tick, from Forge START to END; excludes pacing sleep.
                    serverProcessingTimes[elapsed - 1] = ContinuousAirGameTestPacing.lastProcessingNanos();
                    if (serverProcessingTimes[elapsed - 1] > maxServerProcessing[0]) {
                        maxServerProcessing[0] = serverProcessingTimes[elapsed - 1];
                        maxServerProcessing[1] = elapsed;
                    }
                    if (PROFILE && serverProcessingTimes[elapsed - 1] > 50_000_000L)
                        FHMain.LOGGER.info("THERMAL_SERVER_PROCESSING_SLOW,scene={},observedAtElapsedTicks={},previousTickUs={}",
                                scene, elapsed, serverProcessingTimes[elapsed - 1] / 1000L);
                }
                if (elapsed == endTick) {
                    FHMain.LOGGER.info("THERMAL_TEST_CLOCK_RESULT,scene={},mode={},elapsedTicks={},wallMs={},simulatedMs={},barrierWaitMs={}",
                            scene, ContinuousAirGameTestPacing.clockMode(), elapsed,
                            (System.nanoTime() - startedNanos) / 1_000_000L, elapsed * 50L,
                            (ContinuousAirGameTestPacing.barrierWaitNanos() - startedBarrierNanos) / 1_000_000L);
                    {
                        long[] finished = resources();
                        java.util.Arrays.sort(mainTickTimes);
                        java.util.Arrays.sort(serverProcessingTimes);
                        FHMain.LOGGER.info("THERMAL_RESOURCE_RESULT,scene={},workerCpuMs={},workerAllocatedBytes={},serverCpuMs={},serverAllocatedBytes={},gcCollections={},gcMillis={},mainThermalTickP95Us={},mainThermalTickP99Us={},mainThermalTickMaxUs={},serverProcessingTickP95Us={},serverProcessingTickP99Us={},serverProcessingTickMaxUs={},serverProcessingTickMaxObservedAtElapsedTicks={},heapStartBytes={},heapEndBytes={},heapObservedPeakBytes={},{}",
                                scene, (finished[0] - startedResources[0]) / 1_000_000L, finished[1] - startedResources[1],
                                (finished[2] - startedResources[2]) / 1_000_000L, finished[3] - startedResources[3],
                                finished[4] - startedResources[4], finished[5] - startedResources[5],
                                mainTickTimes[(int) Math.ceil(endTick * .95) - 1] / 1000,
                                mainTickTimes[(int) Math.ceil(endTick * .99) - 1] / 1000, mainTickTimes[endTick - 1] / 1000,
                                serverProcessingTimes[(int) Math.ceil(endTick * .95) - 1] / 1000,
                                serverProcessingTimes[(int) Math.ceil(endTick * .99) - 1] / 1000,
                                serverProcessingTimes[endTick - 1] / 1000,
                                maxServerProcessing[1],
                                startedResources[6], finished[6], Math.max(heapPeak[0], finished[6]),
                                MinecraftThermalInput.observedMainWaits(level));
                    }
                }
                if (elapsed % 200 == 0) heapPeak[0] = Math.max(heapPeak[0],
                        java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                if (elapsed == endTick) {
                    FHMain.LOGGER.info("THERMAL_TIMING_RESULT,scene={},mode=independent_regions,legacyCutTiming=not_applicable,rootPeakChargedBytes={},{}",
                                scene, MinecraftThermalInput.thermalBudgetPeakBytes(), MinecraftThermalInput.describeWorkerProgress(level));
                }
                if (elapsed % 200 == 0 && (PROFILE || OBSERVE_TEMPERATURE_ENVELOPE)) {
                    for (int index = 0; index < probes.length; index++) {
                        BlockPos probe = probes[index];
                        float naturalC = WorldTemperature.naturalAir(level, probe);
                        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe.above(), naturalC, air);
                        FHMain.LOGGER.info("THERMAL_TEMPERATURE_TRACE,scene={},elapsedTicks={},probe={},gameTick={},sampleTick={},airC={},naturalC={},basisTerms={},toleranceC={}",
                                scene, elapsed, index, level.getGameTime(), air.airSampleTick(), air.airTemperatureC(),
                                naturalC, air.airBasisTerms(),
                                MinecraftThermalProfiles.prepare().tuning().continuousAirCorrectionToleranceC());
                    }
                }
                if (firstResponse[0]) return;
                boolean allReadable = true, allCurrent = true;
                for (BlockPos probe : probes) {
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe.above(),
                            WorldTemperature.naturalAir(level, probe), air);
                    if (!sourceField(air) || level.getGameTime() - air.airSampleTick() > 40) {
                        allReadable = false;
                        break;
                    }
                    if (allCurrent) allCurrent = MinecraftThermalInput.describeAirRead(level, probe).startsWith("geometry_current");
                }
                if (!allReadable) return;
                if (!firstLiveRead[0]) {
                    firstLiveRead[0] = true;
                    FHMain.LOGGER.info("THERMAL_FIRST_LIVE_READ,scene={},elapsedTicks={},points={},sampleTick={}",
                            scene, elapsed, probes.length, air.airSampleTick());
                }
                if (!allCurrent) return;
                firstResponse[0] = true;
                FHMain.LOGGER.info("THERMAL_FIRST_RESPONSE,scene={},elapsedTicks={},points={},sampleTick={}",
                        scene, elapsed, probes.length, air.airSampleTick());
        });
    }

    /** Summaries are emitted after the workload, so normal runs need no per-cut text. */
}
