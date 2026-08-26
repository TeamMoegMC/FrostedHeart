/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.atomic.AtomicReference;

class P2PItemTransferTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void transfersInSlotOrderAndHonorsBudgetAndFilter() {
        ItemStackHandler source = new ItemStackHandler(3);
        source.setStackInSlot(0, new ItemStack(Items.STONE, 4));
        source.setStackInSlot(1, new ItemStack(Items.DIRT, 7));
        source.setStackInSlot(2, new ItemStack(Items.STONE, 9));
        ItemStackHandler target = new ItemStackHandler(2);

        P2PItemTransfer.Result result = P2PItemTransfer.move(
                source, target, stack -> stack.is(Items.STONE), 6,
                stack -> { throw new AssertionError("unexpected recovery"); });

        assertEquals(6, result.movedItems());
        assertTrue(source.getStackInSlot(0).isEmpty());
        assertEquals(7, source.getStackInSlot(1).getCount());
        assertEquals(7, source.getStackInSlot(2).getCount());
        assertEquals(6, target.getStackInSlot(0).getCount());
    }

    @Test
    void fullTargetLeavesSourceUntouched() {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.STONE, 12));
        ItemStackHandler target = new ItemStackHandler(1);
        target.setStackInSlot(0, new ItemStack(Items.DIRT, 64));

        P2PItemTransfer.Result result = P2PItemTransfer.move(
                source, target, stack -> true, 12,
                stack -> { throw new AssertionError("unexpected recovery"); });

        assertEquals(0, result.movedItems());
        assertEquals(12, source.getStackInSlot(0).getCount());
        assertEquals(64, target.getStackInSlot(0).getCount());
    }

    @Test
    void changingSourceBetweenSimulationAndCommitCannotDuplicate() {
        ItemStackHandler source = new ItemStackHandler(1) {
            private boolean simulated;

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (simulate) {
                    simulated = true;
                    return super.extractItem(slot, amount, true);
                }
                if (simulated) {
                    setStackInSlot(slot, ItemStack.EMPTY);
                }
                return super.extractItem(slot, amount, false);
            }
        };
        source.setStackInSlot(0, new ItemStack(Items.STONE, 8));
        ItemStackHandler target = new ItemStackHandler(1);

        P2PItemTransfer.Result result = P2PItemTransfer.move(
                source, target, stack -> true, 8,
                stack -> { throw new AssertionError("unexpected recovery"); });

        assertEquals(0, result.movedItems());
        assertTrue(target.getStackInSlot(0).isEmpty());
    }

    @Test
    void partialActualAcceptanceIsReturnedOrRetainedBySender() {
        ItemStackHandler source = new ItemStackHandler(1) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }
        };
        source.setStackInSlot(0, new ItemStack(Items.STONE, 10));
        ItemStackHandler target = new ItemStackHandler(1) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (simulate) {
                    return ItemStack.EMPTY;
                }
                ItemStack limited = stack.copyWithCount(Math.min(3, stack.getCount()));
                ItemStack limitedRemainder = super.insertItem(slot, limited, false);
                int inserted = limited.getCount() - limitedRemainder.getCount();
                ItemStack remainder = stack.copy();
                remainder.shrink(inserted);
                return remainder;
            }
        };
        AtomicReference<ItemStack> retained = new AtomicReference<>(ItemStack.EMPTY);

        P2PItemTransfer.Result result = P2PItemTransfer.move(
                source, target, stack -> true, 10, retained::set);

        assertEquals(3, result.movedItems());
        assertTrue(result.retainedRecoveryStack());
        assertEquals(7, retained.get().getCount());
        assertEquals(3, target.getStackInSlot(0).getCount());
        assertEquals(10, source.getStackInSlot(0).getCount()
                + target.getStackInSlot(0).getCount() + retained.get().getCount());
    }

    @Test
    void independentMovesCanBothCommitToSameTarget() {
        ItemStackHandler firstSource = new ItemStackHandler(1);
        firstSource.setStackInSlot(0, new ItemStack(Items.STONE, 4));
        ItemStackHandler secondSource = new ItemStackHandler(1);
        secondSource.setStackInSlot(0, new ItemStack(Items.DIRT, 3));
        ItemStackHandler target = new ItemStackHandler(2);

        P2PItemTransfer.Result first = P2PItemTransfer.move(
                firstSource, target, stack -> true, 4,
                stack -> { throw new AssertionError("unexpected recovery"); });
        P2PItemTransfer.Result second = P2PItemTransfer.move(
                secondSource, target, stack -> true, 3,
                stack -> { throw new AssertionError("unexpected recovery"); });

        assertEquals(4, first.movedItems());
        assertEquals(3, second.movedItems());
        assertEquals(4, target.getStackInSlot(0).getCount());
        assertEquals(3, target.getStackInSlot(1).getCount());
        assertTrue(firstSource.getStackInSlot(0).isEmpty());
        assertTrue(secondSource.getStackInSlot(0).isEmpty());
    }
}
