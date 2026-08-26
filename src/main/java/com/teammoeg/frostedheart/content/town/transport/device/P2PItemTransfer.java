/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;
import java.util.function.Predicate;

/** Simulation-first item transfer with explicit sender-owned recovery for partial commits. */
public final class P2PItemTransfer {
    private P2PItemTransfer() {
    }

    public static Result move(
            IItemHandler source,
            IItemHandler target,
            Predicate<ItemStack> filter,
            int itemBudget,
            RemainderSink remainderSink
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(remainderSink, "remainderSink");
        int remainingBudget = Math.max(0, itemBudget);
        int moved = 0;

        for (int slot = 0; slot < source.getSlots() && remainingBudget > 0; slot++) {
            ItemStack visible = source.getStackInSlot(slot);
            if (visible.isEmpty() || !filter.test(visible)) {
                continue;
            }
            int requested = Math.min(remainingBudget, visible.getCount());
            ItemStack simulatedExtraction = source.extractItem(slot, requested, true);
            if (simulatedExtraction.isEmpty() || !filter.test(simulatedExtraction)) {
                continue;
            }
            ItemStack simulatedRemainder = insert(target, simulatedExtraction, true);
            int simulatedAccepted = simulatedExtraction.getCount()
                    - simulatedRemainder.getCount();
            if (simulatedAccepted <= 0) {
                continue;
            }

            ItemStack extracted = source.extractItem(slot, simulatedAccepted, false);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack actualRemainder = insert(target, extracted, false);
            int inserted = extracted.getCount() - actualRemainder.getCount();
            if (inserted > 0) {
                moved += inserted;
                remainingBudget -= inserted;
            }
            if (!actualRemainder.isEmpty()) {
                ItemStack notRestored = source.insertItem(slot, actualRemainder, false);
                if (!notRestored.isEmpty()) {
                    remainderSink.retain(notRestored.copy());
                    return new Result(moved, true);
                }
            }
        }
        return new Result(moved, false);
    }

    private static ItemStack insert(IItemHandler target, ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < target.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = target.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    @FunctionalInterface
    public interface RemainderSink {
        /** Must retain the complete stack before returning. */
        void retain(ItemStack stack);
    }

    public record Result(
            int movedItems,
            boolean retainedRecoveryStack
    ) {
    }
}
