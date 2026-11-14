/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/** Optional test-only timing of the exact interval charged by the existing pool. */
public final class ThermalNetworkWorkerTiming {
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final Totals MIXED = new Totals(), RECOVERY = new Totals();
    private static final ThreadLocal<Context> CURRENT = ThreadLocal.withInitial(Context::new);

    private static final class Context {
        Totals totals;
        long engineWall, engineCpu, engineBlocked, engineWaited, engineObservation;
    }
    private static final class Totals {
        final LongAdder tasks = new LongAdder(), wall = new LongAdder(), cpu = new LongAdder();
        final LongAdder blocked = new LongAdder(), waited = new LongAdder(), controlTasks = new LongAdder(), controlWall = new LongAdder();
        final LongAdder engineWall = new LongAdder(), engineCpu = new LongAdder(), engineBlocked = new LongAdder(), engineWaited = new LongAdder();
        final LongAdder observation = new LongAdder();
        Map<String, Object> snapshot() {
            var out = new LinkedHashMap<String, Object>();
            out.put("tasks", tasks.sum()); out.put("taskWallMs", wall.sum() / 1e6); out.put("taskCpuMs", cpu.sum() / 1e6);
            out.put("taskBlockedMs", blocked.sum()); out.put("taskWaitedMs", waited.sum());
            out.put("engineWallMs", engineWall.sum() / 1e6); out.put("engineCpuMs", engineCpu.sum() / 1e6);
            out.put("engineBlockedMs", engineBlocked.sum()); out.put("engineWaitedMs", engineWaited.sum());
            out.put("coordinationWallMs", (wall.sum() - engineWall.sum()) / 1e6);
            out.put("coordinationBlockedMs", blocked.sum() - engineBlocked.sum());
            out.put("controlTasks", controlTasks.sum()); out.put("controlTaskWallMs", controlWall.sum() / 1e6);
            out.put("observationWallMs", observation.sum() / 1e6);
            out.put("contentionSupported", THREADS.isThreadContentionMonitoringSupported());
            out.put("contentionEnabled", THREADS.isThreadContentionMonitoringEnabled());
            out.put("scope", "Diagnostic run only. The same task interval as pool charging, plus engine attribution. CPU and contention counters have platform granularity; observer overhead is measured and affects this run. Phase is selected at task entry.");
            return out;
        }
    }
    public static void start() {
        if (THREADS.isThreadContentionMonitoringSupported() && !THREADS.isThreadContentionMonitoringEnabled())
            THREADS.setThreadContentionMonitoringEnabled(true);
    }
    private static long blocked(ThreadInfo before, ThreadInfo after) {
        return before == null || after == null || before.getBlockedTime() < 0 ? 0 : Math.max(0, after.getBlockedTime() - before.getBlockedTime());
    }
    private static long waited(ThreadInfo before, ThreadInfo after) {
        return before == null || after == null || before.getWaitedTime() < 0 ? 0 : Math.max(0, after.getWaitedTime() - before.getWaitedTime());
    }
    public static void task(Runnable task) {
        int phase = ThermalNetworkLoadServer.workerTimingPhase();
        if (!(task instanceof ThermalWorkerPool.RegionalTask regional) || phase == 0) { task.run(); return; }
        var context = CURRENT.get(); var total = context.totals = phase == 1 ? MIXED : RECOVERY;
        context.engineWall = context.engineCpu = context.engineBlocked = context.engineWaited = context.engineObservation = 0;
        long observed = System.nanoTime(), id = Thread.currentThread().getId();
        boolean control = regional.controlPending();
        var before = THREADS.getThreadInfo(id, 0); long cpu = THREADS.getCurrentThreadCpuTime(), started = System.nanoTime();
        try { task.run(); }
        finally {
            long elapsed = System.nanoTime() - started, usedCpu = THREADS.getCurrentThreadCpuTime() - cpu;
            var after = THREADS.getThreadInfo(id, 0);
            total.tasks.increment(); total.wall.add(elapsed); total.cpu.add(Math.max(0, usedCpu));
            total.blocked.add(blocked(before, after)); total.waited.add(waited(before, after));
            total.engineWall.add(context.engineWall); total.engineCpu.add(context.engineCpu);
            total.engineBlocked.add(context.engineBlocked); total.engineWaited.add(context.engineWaited);
            if (control) { total.controlTasks.increment(); total.controlWall.add(elapsed); }
            total.observation.add(System.nanoTime() - observed - elapsed + context.engineObservation);
            context.totals = null;
        }
    }
    public static void engine(ThermalDimensionEngine engine) {
        var context = CURRENT.get();
        if (context.totals == null) { engine.processWorkRound(); return; }
        long observed = System.nanoTime(), id = Thread.currentThread().getId();
        var before = THREADS.getThreadInfo(id, 0); long cpu = THREADS.getCurrentThreadCpuTime(), started = System.nanoTime();
        try { engine.processWorkRound(); }
        finally {
            long elapsed = System.nanoTime() - started, usedCpu = THREADS.getCurrentThreadCpuTime() - cpu;
            var after = THREADS.getThreadInfo(id, 0);
            context.engineWall += elapsed; context.engineCpu += Math.max(0, usedCpu);
            context.engineBlocked += blocked(before, after); context.engineWaited += waited(before, after);
            context.engineObservation += System.nanoTime() - observed - elapsed;
        }
    }
    public static Map<String, Object> mixed() { return MIXED.snapshot(); }
    public static Map<String, Object> recovery() { return RECOVERY.snapshot(); }
}
