/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_spatial_power")
@PrefixGameTestTemplate(false)
public final class SpatialPowerTimelineGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void eventsChunkCaptureAndCancelledBanksKeepExactDeclaredEnergy(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(4 * 1024 * 1024);
        var a = new SpatialEnergyBanks.Address(BlockPos.asLong(1, 64, 1), -1, 2);
        var b = new SpatialEnergyBanks.Address(BlockPos.asLong(33, 64, 1), -1, 2);
        try (var banks = new SpatialEnergyBanks(budget, 2, 32, 0);
                var power = new SpatialPowerTimeline(budget, banks, 32, 64, 0)) {
            helper.assertTrue(power.tryRegister(1, 1, new SpatialEnergyBanks.Address[]{a, null}, new double[]{.8, .2}, 1000, true, 0)
                    && power.tryRegister(2, 1, new SpatialEnergyBanks.Address[]{b}, new double[]{1}, 600, true, 0), "Register actual finite-face and declared-loss channels");
            power.update(1, 1, 2000, true, 5); power.integrateChunk(a.chunk(), 10);
            near(helper, 600, pending(banks, a.chunk()), "Power changes integrate the old interval before replacing watts");
            near(helper, 0, pending(banks, b.chunk()), "Saving one Chunk does not integrate unrelated producers");
            var sealed = power.seal(20);
            near(helper, 1400, pending(banks, a.chunk()), "Integrate the remaining source interval once");
            near(helper, 600, pending(banks, b.chunk()), "Periodic integration eventually services the unrelated source");
            helper.assertTrue(!power.tryRegister(3, 1, new SpatialEnergyBanks.Address[]{a, new SpatialEnergyBanks.Address(BlockPos.asLong(65, 64, 1), -1)},
                    new double[]{.5, .5}, 1000, true, 20), "A partially prepared source rolls back when every prepaid receiver is occupied");
            power.update(1, 1, 0, false, 25);
            banks.cancel(sealed.sequence);
            var retry = power.seal(40);
            helper.assertTrue(retry.fromTick == 0, "A cancelled epoch retries the entire uncommitted input interval");
            near(helper, 1800, pending(banks, a.chunk()), "Both claimed and newer source input survive cancellation once");
            power.remove(1, 1, 40);
            helper.assertTrue(power.tryRegister(1, 2, new SpatialEnergyBanks.Address[]{a}, new double[]{1}, 1000, true, 40), "A new incarnation can share the existing spatial receiver");
            helper.assertTrue(!power.update(1, 1, 1e6, true, 40), "A late retired producer cannot change the replacement's power");
            power.integrate(60);
            near(helper, 2800, pending(banks, a.chunk()), "Only the current producer adds new input");
            near(helper, 1800, pending(banks, b.chunk()), "The unrelated producer continues its full interval");
            near(helper, 5050, power.producedJ(), "Declared production accounts for all source intervals");
            near(helper, power.producedJ(), pending(banks, a.chunk()) + pending(banks, b.chunk()) + power.declaredLossJ(), "Spatial J plus declared loss equals produced J");
            near(helper, 0, power.unavailableReceiverJ(), "No accepted source lost its receiver");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Source metadata, directories and both banks release");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void repeatedPowerEventsAllocateNoHistoryAndColdReplayFinishesOnce(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(4 * 1024 * 1024);
        var address = new SpatialEnergyBanks.Address(BlockPos.asLong(1, 64, 1), -1, 2);
        long sourceId = BlockPos.asLong(1000, 64, -2000);
        try (var banks = new SpatialEnergyBanks(budget, 4, 32, 0);
                var power = new SpatialPowerTimeline(budget, banks, 32, 64, 0)) {
            helper.assertTrue(power.tryRegister(sourceId, 1, new SpatialEnergyBanks.Address[]{address}, new double[]{1}, 1000, true, 0), "Register one producer with an ordinary uncached world-position id");
            var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
            if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().getId();
            for (int read = 0; read < 20; read++) bean.getThreadAllocatedBytes(thread);
            // Warm the same invocation/loop as the measured pass. Separate inlined
            // loops can deoptimise at their first back edge and materialise a VM
            // frame, which is not a producer history allocation.
            for (int pass = 0; pass < 8; pass++) powerEvents(power, sourceId, pass * 10000);
            long before = bean.getThreadAllocatedBytes(thread);
            powerEvents(power, sourceId, 80000);
            long allocated = bean.getThreadAllocatedBytes(thread) - before;
            helper.assertTrue(allocated == 0, "Power changes create no per-event window, span, map or callback; allocated=" + allocated);
            long start = 90000;
            power.update(sourceId, 1, 0, false, start); power.integrate(start);
            double baseline = pending(banks, address.chunk());
            helper.assertTrue(power.tryReplay(address, start, start + 80, 4000), "Admit one finite cold replay");
            power.integrate(start + 20);
            near(helper, baseline + 1000, pending(banks, address.chunk()), "An archive is spread over its original finite replay interval");
            double[] future = {0};
            power.pendingReplaysInChunk(address.chunk(), (target, from, to, joules) -> {
                helper.assertTrue(from == start + 20 && to == start + 80, "Save keeps the undelivered replay interval"); future[0] += joules;
            });
            near(helper, 3000, future[0], "The replay tail remains separately saveable");
            power.integrate(start + 80); power.integrate(start + 100);
            near(helper, baseline + 4000, pending(banks, address.chunk()), "Completed archives never produce a second time");
            helper.assertTrue(power.replayCount() == 0, "Finished replay owners leave the active input directory");
            FHMain.LOGGER.info("THERMAL_SPATIAL_POWER_RESULT,powerEvents=10000,eventAllocatedBytes={},replayedJ={},unavailableJ={}",
                    allocated, power.replayedJ(), power.unavailableReceiverJ());
        }
        helper.assertTrue(budget.usedBytes() == 0, "Cold replay storage releases");
        helper.succeed();
    }

    private static void powerEvents(SpatialPowerTimeline power, long sourceId, int from) {
        for (int offset = 1; offset <= 10000; offset++)
            power.update(sourceId, 1, offset % 2 == 0 ? 1000 : 2000, true, from + offset);
    }

    private static double pending(SpatialEnergyBanks banks, long chunk) {
        double[] result = {0}; banks.pendingInChunk(chunk, (target, joules) -> result[0] += joules); return result[0];
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-8 * Math.max(1, Math.abs(expected)), message + "; expected=" + expected + ",actual=" + actual);
    }
}
