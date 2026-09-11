/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.resource.action.IActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import net.minecraft.world.item.ItemStack;

/** Budgeted item-transfer core kept independent from block-entity registration for deterministic tests. */
final class WarehouseInterfaceTransfer {
    private WarehouseInterfaceTransfer() {
    }

    static Result balance(
            InventoryAccess inventory,
            WarehouseInterfaceTarget[] targets,
            boolean outputAllowed,
            IActionExecutorHandler executor,
            int tickBudget
    ) {
        boolean changed = false;
        int remainingBudget = Math.max(0, tickBudget);
        int movedTotal = 0;

        for (int slot = 0; slot < WarehouseInterfaceBlockEntity.SLOT_COUNT && remainingBudget > 0; slot++) {
            ItemStack current = inventory.getStack(slot);
            if (current.isEmpty()) {
                continue;
            }
            WarehouseInterfaceTarget target = targets[slot];
            int amountToExport = target == null || !target.matches(current)
                    ? current.getCount()
                    : Math.max(0, current.getCount() - target.amount());
            if (amountToExport <= 0) {
                continue;
            }

            int requestedAmount = Math.min(amountToExport, remainingBudget);
            ItemStack offered = current.copyWithCount(requestedAmount);
            TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(
                    offered, ResourceActionType.ADD, ResourceActionMode.MAXIMIZE);
            TownResourceActionResults.ItemStackActionResult result = executor.execute(action);
            int moved = Math.min(requestedAmount, result.itemStackModified().getCount());
            if (moved > 0) {
                ItemStack remainder = current.copy();
                remainder.shrink(moved);
                inventory.setStack(slot, remainder);
                changed = true;
                movedTotal += moved;
                remainingBudget -= moved;
            }
        }

        for (int slot = 0; slot < WarehouseInterfaceBlockEntity.SLOT_COUNT
                && outputAllowed && remainingBudget > 0; slot++) {
            WarehouseInterfaceTarget target = targets[slot];
            if (target == null) {
                continue;
            }
            ItemStack current = inventory.getStack(slot);
            if (!current.isEmpty() && !target.matches(current)) {
                continue;
            }
            int deficit = target.amount() - current.getCount();
            if (deficit <= 0) {
                continue;
            }

            int requestedAmount = Math.min(deficit, remainingBudget);
            ItemStack requested = target.key().toStack(requestedAmount);
            TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(
                    requested, ResourceActionType.COST, ResourceActionMode.MAXIMIZE);
            TownResourceActionResults.ItemStackActionResult result = executor.execute(action);
            ItemStack extracted = result.itemStackModified();
            if (extracted.isEmpty()) {
                continue;
            }

            int moved = Math.min(requestedAmount, extracted.getCount());
            ItemStack filled = current.isEmpty() ? extracted.copyWithCount(moved) : current.copy();
            if (!current.isEmpty()) {
                filled.grow(moved);
            }
            inventory.setStack(slot, filled);
            changed = true;
            movedTotal += moved;
            remainingBudget -= moved;
        }

        return new Result(movedTotal, hasDemand(inventory, targets, outputAllowed), changed);
    }

    static boolean hasDemand(
            InventoryAccess inventory,
            WarehouseInterfaceTarget[] targets,
            boolean outputAllowed
    ) {
        for (int slot = 0; slot < WarehouseInterfaceBlockEntity.SLOT_COUNT; slot++) {
            ItemStack current = inventory.getStack(slot);
            WarehouseInterfaceTarget target = targets[slot];
            if (!current.isEmpty()
                    && (target == null || !target.matches(current) || current.getCount() > target.amount())) {
                return true;
            }
            if (outputAllowed && target != null
                    && (current.isEmpty() || target.matches(current))
                    && current.getCount() < target.amount()) {
                return true;
            }
        }
        return false;
    }

    interface InventoryAccess {
        ItemStack getStack(int slot);

        void setStack(int slot, ItemStack stack);
    }

    record Result(int movedItems, boolean hasRemainingWork, boolean inventoryChanged) {
    }
}
