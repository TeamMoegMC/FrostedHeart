/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.teammoeg.frostedheart.content.town.transport.P2PDirectedBinding;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.UUID;
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
    void stableRoundRobinGivesEveryIncomingSourceTurns() {
        ResourceKey<Level> dimension = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("frostedheart", "test"));
        P2PTerminalEndpoint receiver = endpoint(dimension, 0, P2PTerminalRole.RECEIVING);
        P2PTerminalEndpoint first = endpoint(dimension, 1, P2PTerminalRole.SHIPPING);
        P2PTerminalEndpoint second = endpoint(dimension, 2, P2PTerminalRole.SHIPPING);
        P2PDirectedBinding a = new P2PDirectedBinding(
                new UUID(0, 2), first, receiver, 16, false);
        P2PDirectedBinding b = new P2PDirectedBinding(
                new UUID(0, 1), second, receiver, 16, false);

        List<P2PDirectedBinding> reverseMapOrder = List.of(a, b);
        assertTrue(P2PFairTransferScheduler.isSenderTurn(reverseMapOrder, second.pos(), 0));
        assertTrue(P2PFairTransferScheduler.isSenderTurn(reverseMapOrder, first.pos(), 1));
        assertFalse(P2PFairTransferScheduler.isSenderTurn(reverseMapOrder, first.pos(), 2));
        assertTrue(P2PFairTransferScheduler.isSenderTurn(reverseMapOrder, second.pos(), 2));
    }

    private static P2PTerminalEndpoint endpoint(
            ResourceKey<Level> dimension,
            int x,
            P2PTerminalRole role
    ) {
        return new P2PTerminalEndpoint(
                GlobalPos.of(dimension, new BlockPos(x, 0, 0)), role);
    }
}
