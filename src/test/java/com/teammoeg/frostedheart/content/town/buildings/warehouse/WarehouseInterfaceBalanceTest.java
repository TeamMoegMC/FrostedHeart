/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.resource.action.AbstractActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseInterfaceBalanceTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void exportsAcrossSlotsShareOneTickBudget() {
        ItemStackHandler inventory = new ItemStackHandler(WarehouseInterfaceBlockEntity.SLOT_COUNT);
        WarehouseInterfaceTarget[] targets = new WarehouseInterfaceTarget[WarehouseInterfaceBlockEntity.SLOT_COUNT];
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        inventory.setStackInSlot(1, new ItemStack(Items.DIRT, 10));
        targets[1] = WarehouseInterfaceTarget.fromStack(new ItemStack(Items.DIRT)).withAmount(5);
        RecordingItemHandler handler = new RecordingItemHandler(Integer.MAX_VALUE);

        WarehouseInterfaceTransfer.Result result = WarehouseInterfaceTransfer.balance(
                access(inventory), targets, false, handler, 12);

        assertEquals(List.of(10, 2), handler.requestedCounts);
        assertEquals(12, result.movedItems());
        assertEquals(0, inventory.getStackInSlot(0).getCount());
        assertEquals(8, inventory.getStackInSlot(1).getCount());
        assertTrue(result.hasRemainingWork());
    }

    @Test
    void partialActionSuccessOnlyConsumesActuallyMovedBudgetAndItems() {
        ItemStackHandler inventory = new ItemStackHandler(WarehouseInterfaceBlockEntity.SLOT_COUNT);
        WarehouseInterfaceTarget[] targets = new WarehouseInterfaceTarget[WarehouseInterfaceBlockEntity.SLOT_COUNT];
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        RecordingItemHandler handler = new RecordingItemHandler(3);

        WarehouseInterfaceTransfer.Result result = WarehouseInterfaceTransfer.balance(
                access(inventory), targets, false, handler, 8);

        assertEquals(List.of(8), handler.requestedCounts);
        assertEquals(3, result.movedItems());
        assertEquals(7, inventory.getStackInSlot(0).getCount());
        assertTrue(result.hasRemainingWork());
    }

    @Test
    void exportAndRestockCompeteForTheSameBudget() {
        ItemStackHandler inventory = new ItemStackHandler(WarehouseInterfaceBlockEntity.SLOT_COUNT);
        WarehouseInterfaceTarget[] targets = new WarehouseInterfaceTarget[WarehouseInterfaceBlockEntity.SLOT_COUNT];
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 4));
        targets[1] = WarehouseInterfaceTarget.fromStack(new ItemStack(Items.DIRT)).withAmount(5);
        RecordingItemHandler handler = new RecordingItemHandler(Integer.MAX_VALUE);

        WarehouseInterfaceTransfer.Result result = WarehouseInterfaceTransfer.balance(
                access(inventory), targets, true, handler, 6);

        assertEquals(List.of(ResourceActionType.ADD, ResourceActionType.COST), handler.actionTypes);
        assertEquals(List.of(4, 2), handler.requestedCounts);
        assertEquals(6, result.movedItems());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertEquals(2, inventory.getStackInSlot(1).getCount());
        assertTrue(result.hasRemainingWork());
    }

    @Test
    void zeroActionResultPreservesItemsAndContinuation() {
        ItemStackHandler inventory = new ItemStackHandler(WarehouseInterfaceBlockEntity.SLOT_COUNT);
        WarehouseInterfaceTarget[] targets = new WarehouseInterfaceTarget[WarehouseInterfaceBlockEntity.SLOT_COUNT];
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        RecordingItemHandler handler = new RecordingItemHandler(0);

        WarehouseInterfaceTransfer.Result result = WarehouseInterfaceTransfer.balance(
                access(inventory), targets, false, handler, 8);

        assertEquals(List.of(8), handler.requestedCounts);
        assertEquals(0, result.movedItems());
        assertEquals(10, inventory.getStackInSlot(0).getCount());
        assertTrue(result.hasRemainingWork());
        assertFalse(result.inventoryChanged());
    }

    @Test
    void satisfiedTargetSubmitsNoResourceAction() {
        ItemStackHandler inventory = new ItemStackHandler(WarehouseInterfaceBlockEntity.SLOT_COUNT);
        WarehouseInterfaceTarget[] targets = new WarehouseInterfaceTarget[WarehouseInterfaceBlockEntity.SLOT_COUNT];
        inventory.setStackInSlot(0, new ItemStack(Items.DIRT, 5));
        targets[0] = WarehouseInterfaceTarget.fromStack(new ItemStack(Items.DIRT)).withAmount(5);
        RecordingItemHandler handler = new RecordingItemHandler(Integer.MAX_VALUE);

        WarehouseInterfaceTransfer.Result result = WarehouseInterfaceTransfer.balance(
                access(inventory), targets, true, handler, 8);

        assertTrue(handler.requestedCounts.isEmpty());
        assertEquals(0, result.movedItems());
        assertFalse(result.hasRemainingWork());
        assertFalse(result.inventoryChanged());
    }

    @Test
    void thousandsOfIdleInterfacesCauseNoActionInventoryChangeOrVisualWrite() {
        WarehouseInterfaceTarget[] targets = new WarehouseInterfaceTarget[WarehouseInterfaceBlockEntity.SLOT_COUNT];
        RecordingItemHandler handler = new RecordingItemHandler(Integer.MAX_VALUE);
        int inventoryChanges = 0;
        int remainingWork = 0;
        int visualWrites = 0;

        for (int index = 0; index < 4096; index++) {
            ItemStackHandler inventory = new ItemStackHandler(WarehouseInterfaceBlockEntity.SLOT_COUNT);
            WarehouseInterfaceTransfer.Result result = WarehouseInterfaceTransfer.balance(
                    access(inventory), targets, true, handler, 64);
            inventoryChanges += result.inventoryChanged() ? 1 : 0;
            remainingWork += result.hasRemainingWork() ? 1 : 0;
            visualWrites += WarehouseInterfaceBlock.shouldUpdateTransportVisualState(
                    WarehouseInterfaceBlock.TransportVisualState.ACTIVE,
                    WarehouseInterfaceBlock.TransportVisualState.ACTIVE) ? 1 : 0;
        }

        assertTrue(handler.requestedCounts.isEmpty(), "idle interfaces must submit no inventory Action");
        assertEquals(0, inventoryChanges, "the BE setChanged gate must stay closed");
        assertEquals(0, remainingWork, "idle interfaces must not schedule continuation work");
        assertEquals(0, visualWrites, "unchanged transport visuals must not write BlockState");
    }

    private static WarehouseInterfaceTransfer.InventoryAccess access(ItemStackHandler inventory) {
        return new WarehouseInterfaceTransfer.InventoryAccess() {
            @Override
            public ItemStack getStack(int slot) {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public void setStack(int slot, ItemStack stack) {
                inventory.setStackInSlot(slot, stack);
            }
        };
    }

    private static final class RecordingItemHandler extends AbstractActionExecutorHandler {
        private final int maximumSuccess;
        private final List<Integer> requestedCounts = new ArrayList<>();
        private final List<ResourceActionType> actionTypes = new ArrayList<>();

        private RecordingItemHandler(int maximumSuccess) {
            this.maximumSuccess = maximumSuccess;
            registerExecutor(TownResourceActions.ItemStackAction.class, this::executeItemAction);
        }

        private TownResourceActionResults.ItemStackActionResult executeItemAction(
                TownResourceActions.ItemStackAction action
        ) {
            int requested = action.itemToModify().getCount();
            requestedCounts.add(requested);
            actionTypes.add(action.actionType());
            int moved = Math.min(requested, maximumSuccess);
            ItemStack modified = action.itemToModify().copyWithCount(moved);
            ItemStack left = action.itemToModify().copyWithCount(requested - moved);
            if (left.getCount() == 0) {
                left = ItemStack.EMPTY;
            }
            return new TownResourceActionResults.ItemStackActionResult(
                    action, moved == requested, modified, left);
        }

    }
}
