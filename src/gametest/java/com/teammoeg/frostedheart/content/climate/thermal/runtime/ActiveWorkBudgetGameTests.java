/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_active_work_budget")
@PrefixGameTestTemplate(false)
public final class ActiveWorkBudgetGameTests {
    private static volatile long result;

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void waitingIsExcludedAndNestedParallelWorkKeepsItsOwner(GameTestHelper helper) throws InterruptedException {
        var parent = ThermalWorkBudget.newAccount();
        var child = ThermalWorkBudget.newAccount();
        ThermalWorkBudget.Context context;
        try (var outer = ThermalWorkBudget.enter(parent, ThermalWorkBudget.Phase.AIR)) {
            calculate();
            try (var nested = ThermalWorkBudget.enter(child, ThermalWorkBudget.Phase.MATERIAL)) {
                long pausedParent = parent.activeNanos();
                calculate();
                ThermalWorkBudget.checkpoint();
                helper.assertTrue(parent.activeNanos() == pausedParent, "A nested owner cannot charge its paused parent");
            }
            ThermalWorkBudget.phase(ThermalWorkBudget.Phase.CONTACTS);
            calculate();
            context = ThermalWorkBudget.capture();
        }
        long firstIntervals = parent.activeNanos();
        long waitingStarted = System.nanoTime();
        while (System.nanoTime() - waitingStarted < 20_000_000L) LockSupport.parkNanos(1_000_000L);
        helper.assertTrue(parent.activeNanos() == firstIntervals, "Time between execution intervals is never charged");
        helper.assertTrue(ThermalWorkBudget.capture() == null && child.activeNanos() > 0,
                "Closing nested intervals restores and then releases the thread context");

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();
        Thread[] helpers = new Thread[2];
        for (int index = 0; index < helpers.length; index++) {
            helpers[index] = new Thread(() -> {
                try {
                    ready.countDown(); start.await();
                    try (var scope = ThermalWorkBudget.attach(context)) { calculate(); }
                    if (ThermalWorkBudget.capture() != null) throw new AssertionError("Helper retained its work context");
                } catch (Throwable problem) { failure.compareAndSet(null, problem); }
            }, "thermal-active-budget-fixture-" + index);
            helpers[index].start();
        }
        ready.await(); start.countDown();
        for (Thread thread : helpers) thread.join();
        helper.assertTrue(failure.get() == null, "Both real helper intervals complete: " + failure.get());
        helper.assertTrue(parent.activeNanos() > firstIntervals && parent.activeNanos(ThermalWorkBudget.Phase.CONTACTS) > 0,
                "Helpers charge the captured owner's phase on their actual execution threads");
        long phases = 0;
        for (var phase : ThermalWorkBudget.Phase.values()) phases += parent.activeNanos(phase);
        helper.assertTrue(phases == parent.activeNanos(), "Exclusive phase time equals total work across both helpers");
        FHMain.LOGGER.info("THERMAL_ACTIVE_WORK_BUDGET,parentActiveNs={},childActiveNs={},phaseSumNs={},waitingExcluded=true,nestedOwnership=true,helpers=2",
                parent.activeNanos(), child.activeNanos(), phases);
        helper.succeed();
    }

    private static void calculate() {
        long value = 1, started = System.nanoTime();
        do {
            for (int index = 0; index < 1024; index++) value = Long.rotateLeft(value * 31 + index, 7);
        } while (System.nanoTime() - started < 1_000_000L);
        result = value;
    }
}
