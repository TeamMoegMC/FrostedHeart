/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.teammoeg.frostedheart.FHMain;

/** Opt-in GameTest diagnostics. Cycles are kept as cycles, never converted into CPU seconds. */
public final class ThermalNativeCpuObservation {
    public static final boolean ENABLED = "true".equalsIgnoreCase(System.getenv("FH_THERMAL_NATIVE_CPU_OBSERVATION"));
    public interface WindowsCpu extends StdCallLibrary {
        int GetCurrentThreadId();
        Pointer OpenThread(int access, boolean inherit, int threadId);
        boolean CloseHandle(Pointer handle);
        boolean QueryThreadCycleTime(Pointer handle, long[] cycles);
        boolean GetThreadTimes(Pointer handle, long[] created, long[] exited, long[] kernel, long[] user);
    }
    private static final WindowsCpu WINDOWS = ENABLED && Platform.isWindows() ? Native.load("kernel32", WindowsCpu.class) : null;
    private static final Map<Long, Counter> COUNTERS = new LinkedHashMap<>();
    private static final ThreadLocal<Boolean> REGISTERED = ThreadLocal.withInitial(() -> false);
    private static final class Counter {
        final String name;
        final Pointer handle;
        final long[] created = new long[1], exited = new long[1], kernel = new long[1], user = new long[1], cycles = new long[1];
        long startCycles, startCpu, previousCycles, previousCpu;
        boolean available;
        Counter(String name, Pointer handle) { this.name = name; this.handle = handle; read(); reset(); }
        void read() {
            available = handle != null && WINDOWS.GetThreadTimes(handle, created, exited, kernel, user)
                    && WINDOWS.QueryThreadCycleTime(handle, cycles);
        }
        long cpu() { return (kernel[0] + user[0]) * 100; }
        void reset() { startCycles = previousCycles = cycles[0]; startCpu = previousCpu = cpu(); }
    }
    public static void registerCurrent() {
        if (WINDOWS == null || REGISTERED.get()) return;
        Thread thread = Thread.currentThread();
        if (!thread.getName().startsWith("frosted-heart-thermal") && !thread.getName().equals("Server thread")) return;
        synchronized (COUNTERS) {
            COUNTERS.computeIfAbsent(thread.getId(), ignored -> new Counter(thread.getName(),
                    WINDOWS.OpenThread(0x0800, false, WINDOWS.GetCurrentThreadId())));
        }
        REGISTERED.set(true);
    }
    public static void begin() {
        if (WINDOWS == null) return;
        registerCurrent();
        synchronized (COUNTERS) { for (Counter counter : COUNTERS.values()) { counter.read(); counter.reset(); } }
    }
    public static void sample(String scene, int elapsed, boolean finished) {
        if (WINDOWS == null) return;
        long workerCycles = 0, workerCpu = 0, totalWorkerCycles = 0, totalWorkerCpu = 0;
        int workers = 0;
        synchronized (COUNTERS) {
            for (Counter counter : COUNTERS.values()) {
                counter.read();
                if (!counter.available) continue;
                long cycles = counter.cycles[0] - counter.previousCycles, cpu = counter.cpu() - counter.previousCpu;
                long totalCycles = counter.cycles[0] - counter.startCycles, totalCpu = counter.cpu() - counter.startCpu;
                if (counter.name.startsWith("frosted-heart-thermal")) {
                    workers++; workerCycles += cycles; workerCpu += cpu; totalWorkerCycles += totalCycles; totalWorkerCpu += totalCpu;
                }
                FHMain.LOGGER.info("THERMAL_NATIVE_CPU_THREAD,scene={},elapsedTicks={},thread={},cycles={},platformCpuNs={},totalCycles={},totalPlatformCpuNs={}",
                        scene, elapsed, counter.name, cycles, cpu, totalCycles, totalCpu);
                counter.previousCycles = counter.cycles[0]; counter.previousCpu = counter.cpu();
            }
        }
        FHMain.LOGGER.info("THERMAL_NATIVE_CPU_{},scene={},elapsedTicks={},workers={},cycles={},platformCpuNs={},totalCycles={},totalPlatformCpuNs={}",
                finished ? "RESULT" : "WINDOW", scene, elapsed, workers, workerCycles, workerCpu, totalWorkerCycles, totalWorkerCpu);
    }
    public static void close() {
        if (WINDOWS == null) return;
        synchronized (COUNTERS) {
            for (Counter counter : COUNTERS.values()) if (counter.handle != null) WINDOWS.CloseHandle(counter.handle);
            COUNTERS.clear();
        }
        REGISTERED.remove();
    }
}
