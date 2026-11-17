/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_spatial_energy_banks")
@PrefixGameTestTemplate(false)
public final class SpatialEnergyBankGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void acceptedDebtCaptureAndExportExcludeUnconfirmedClaimCopies(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1024 * 1024);
        try (var banks = new SpatialEnergyBanks(budget, 4, 16, 0)) {
            var a = new SpatialEnergyBanks.Address(BlockPos.asLong(1, 64, 1), -1);
            var b = new SpatialEnergyBanks.Address(BlockPos.asLong(33, 64, 1), -1);
            long first = banks.retain(a), second = banks.retain(b);
            banks.add(first, 100);
            var initial = banks.seal(20); initial.defer((int) first, 40); banks.commit(initial.sequence);
            banks.add(first, 25); banks.add(second, 60);
            var claimed = banks.seal(40);
            double[] saved = {0}; banks.acceptedPendingInChunk(a.chunk(), (target, joules, since) -> saved[0] += joules);
            helper.assertTrue(saved[0] == 40 && pending(banks, a.chunk()) == 65,
                    "Native store capture adds only 40J Accepted debt, excluding the claim's 25J copy");
            banks.cancel(claimed.sequence); banks.discardPreparedCopy();
            helper.assertTrue(pending(banks, a.chunk()) == 40 && pending(banks, b.chunk()) == 0,
                    "Returning store ownership discards both unconfirmed copies and preserves Accepted debt");
            helper.assertTrue(banks.exportAcceptedPendingInChunk(a.chunk()) == 40
                            && banks.exportAcceptedPendingInChunk(a.chunk()) == 0 && pending(banks, a.chunk()) == 0,
                    "A successful chunk handoff exports its Accepted debt once");
            banks.release(first); banks.release(second);
        }
        helper.assertTrue(budget.usedBytes() == 0, "Accepted debt handoff releases all input storage");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void claimedEnergySurvivesSaveCancelPendingAndSourceRetirement(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1024 * 1024);
        try (var banks = new SpatialEnergyBanks(budget, 4, 16, 0)) {
            var address = new SpatialEnergyBanks.Address(BlockPos.asLong(1, 64, 1), -1);
            long receiver = banks.retain(address);
            helper.assertTrue(receiver != SpatialEnergyBanks.NO_RECEIVER && banks.add(receiver, 100), "Accept the first spatial input");
            var claimed = banks.seal(20);
            helper.assertTrue(banks.oldestUncommittedTick() == 0, "Claiming input does not conceal its outstanding age");
            helper.assertTrue(claimed != null && banks.add(receiver, 50), "The live bank accepts later energy while a worker owns the first bank");
            claimed.defer(0, 100);
            helper.assertTrue(pending(banks, address.chunk()) == 150, "Saving sees claimed and new input, never the unfinished worker trial");
            long appended = banks.retain(new SpatialEnergyBanks.Address(BlockPos.asLong(2, 64, 1), -1));
            helper.assertTrue(appended != SpatialEnergyBanks.NO_RECEIVER && claimed.size == 1 && claimed.joules(0) == 100,
                    "A prepaid receiver appends outside the worker's immutable input range");
            banks.release(appended);
            banks.cancel(claimed.sequence);
            helper.assertTrue(banks.oldestUncommittedTick() == 0, "Cancellation retains the earliest uncommitted interval");
            helper.assertTrue(pending(banks, address.chunk()) == 150, "Cancellation returns the claimed input once, without publishing its trial balance");
            var replacement = banks.seal(40);
            helper.assertTrue(replacement.fromTick == 0 && replacement.joules(0) == 150, "Retry covers the entire uncommitted interval");
            replacement.defer(0, 30);
            banks.release(receiver);
            banks.commit(replacement.sequence);
            helper.assertTrue(banks.oldestUncommittedTick() == 0, "Committing pending J must not relabel old heating debt as fresh input");
            helper.assertTrue(pending(banks, address.chunk()) == 30 && banks.receiverCount() == 1,
                    "After the field takes 120J, only its 30J accepted pending balance survives source retirement");
            var settled = banks.seal(60);
            helper.assertTrue(settled.pendingJoules(0) == 30 && settled.joules(0) == 0 && settled.pendingFromTick(0) == 0,
                    "The next trial receives the pending balance and its original age without new production");
            banks.commit(settled.sequence);
            helper.assertTrue(banks.receiverCount() == 0 && pending(banks, address.chunk()) == 0,
                    "Settlement releases the orphaned receiver");
            helper.assertTrue(banks.oldestUncommittedTick() == Long.MAX_VALUE, "Settled receivers leave no phantom backlog age");
            long reused = banks.retain(new SpatialEnergyBanks.Address(BlockPos.asLong(32, 64, 1), -1));
            helper.assertTrue(reused != receiver && !banks.add(receiver, 999) && banks.add(reused, 5),
                    "A late old-generation callback cannot write into a recycled receiver");
            helper.assertTrue(pending(banks, address.chunk()) == 0, "The recycled address no longer belongs to the old save chunk");
            long[] savedSince = {Long.MAX_VALUE};
            banks.pendingTimedInChunk(new SpatialEnergyBanks.Address(BlockPos.asLong(32, 64, 1), -1).chunk(),
                    (target, joules, oldest) -> savedSince[0] = Math.min(savedSince[0], oldest));
            helper.assertTrue(savedSince[0] == 60 && banks.oldestUncommittedTick() == 60,
                    "A recycled receiver gets the new integration interval, not the previous address's age");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Input arrays and directories release their ownership");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sharedReceiversAndGrowthRespectServerCapacity(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1024 * 1024);
        try (var banks = new SpatialEnergyBanks(budget, 1, 2, 0)) {
            var a = new SpatialEnergyBanks.Address(BlockPos.asLong(1, 64, 1), -1);
            long first = banks.retain(a), shared = banks.retain(a);
            helper.assertTrue(first == shared && banks.receiverCount() == 1, "Sources sharing one address share one receiver");
            banks.release(first);
            helper.assertTrue(banks.add(shared, 10), "One source leaving does not remove the other's input");
            try (var pressure = budget.tryReserve(budget.limitBytes() - budget.usedBytes())) {
                helper.assertTrue(pressure != null && banks.retain(new SpatialEnergyBanks.Address(BlockPos.asLong(2, 64, 1), -1)) == SpatialEnergyBanks.NO_RECEIVER,
                        "Receiver growth cannot use completion credit beyond the shared budget");
                helper.assertTrue(budget.peakUsedBytes() == budget.limitBytes(), "Refused growth leaves no uncharged replacement arrays");
            }
            helper.assertTrue(banks.retain(new SpatialEnergyBanks.Address(BlockPos.asLong(2, 64, 1), -1)) != SpatialEnergyBanks.NO_RECEIVER,
                    "A real capacity release allows receiver growth");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Both old and replacement input storage release exactly once");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sealedAppendNeverReusesVisibleSlotsOrGrowsBorrowedArrays(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1024 * 1024);
        try (var banks = new SpatialEnergyBanks(budget, 3, 8, 0)) {
            var a = new SpatialEnergyBanks.Address(BlockPos.asLong(1, 64, 1), -1);
            var b = new SpatialEnergyBanks.Address(BlockPos.asLong(33, 64, 1), -1);
            var c = new SpatialEnergyBanks.Address(BlockPos.asLong(65, 64, 1), -1);
            var d = new SpatialEnergyBanks.Address(BlockPos.asLong(97, 64, 1), -1);
            long abandoned = banks.retain(a), first = banks.retain(b);
            banks.release(abandoned);
            banks.add(first, 100);
            var input = banks.seal(20);
            long reserved = banks.reservedBytes();
            long appended = banks.retain(c);
            helper.assertTrue(appended != SpatialEnergyBanks.NO_RECEIVER && (int) appended >= input.size
                            && banks.add(appended, 60) && banks.reservedBytes() == reserved,
                    "An in-flight receiver uses only capacity already paid for outside the sealed range");
            helper.assertTrue(banks.retain(d) == SpatialEnergyBanks.NO_RECEIVER && input.address(0) == null
                            && input.address((int) first).equals(b) && input.joules((int) first) == 100,
                    "No sealed slot or borrowed array is replaced, even when an old free slot exists");
            banks.commit(input.sequence);
            helper.assertTrue(pending(banks, b.chunk()) == 0 && pending(banks, c.chunk()) == 60,
                    "Committing the old bank leaves the appended receiver's newer input untouched");
            long reused = banks.retain(d);
            helper.assertTrue((int) reused == (int) abandoned && reused != abandoned && !banks.add(abandoned, 999),
                    "After the worker releases its range, the old hole is reusable with a fresh generation");
            banks.add(reused, 40);
            var next = banks.seal(40);
            helper.assertTrue(next.joules((int) appended) == 60 && next.joules((int) reused) == 40,
                    "The next interval includes both kinds of newly admitted receiver exactly once");
            banks.commit(next.sequence);
        }
        helper.assertTrue(budget.usedBytes() == 0, "Appending and later recycling leave no retained input ownership");
        helper.succeed();
    }

    private static double pending(SpatialEnergyBanks banks, long chunk) {
        double[] total = {0};
        banks.pendingInChunk(chunk, (address, joules) -> total[0] += joules);
        return total[0];
    }
}
