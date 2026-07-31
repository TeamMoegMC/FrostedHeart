/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.ITown;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.ITownWithResources;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;
import com.teammoeg.frostedheart.content.town.resource.action.IActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class WarehouseInterfaceBlockEntity extends CBlockEntity implements CTickableBlockEntity, MenuProvider {
    public static final int SLOT_COUNT = 9;
    public static final int STATUS_UNBOUND = 0;
    public static final int STATUS_UNAVAILABLE = 1;
    public static final int STATUS_WORKING = 2;

    private final LazyTickWorker balanceWorker = new LazyTickWorker(10, this::validateAndBalance);
    private WarehouseRedstoneMode redstoneMode = WarehouseRedstoneMode.IGNORE;
    private boolean suppressInventoryCallback;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!suppressInventoryCallback && level != null) {
                WarehouseInterfaceBlockEntity.this.setChanged();
                if (!level.isClientSide) {
                    balanceWorker.enqueue();
                }
            }
        }
    };
    private final LazyOptional<IItemHandler> inventoryCapability = LazyOptional.of(() -> inventory);
    private final WarehouseInterfaceTarget[] targets = new WarehouseInterfaceTarget[SLOT_COUNT];

    private ITownProviderSerializable<? extends ITownWithBuildings> townProvider;
    private BlockPos warehousePos;
    private int connectionStatus = STATUS_UNBOUND;

    public WarehouseInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WAREHOUSE_INTERFACE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Nullable
    public WarehouseInterfaceTarget getTarget(int slot) {
        return isSlotValid(slot) ? targets[slot] : null;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public WarehouseRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    /**
     * 循环切换红石控制模式，并立即重新评估补货。
     * <p>
     * Cycles the redstone control mode and re-evaluates balancing right away.
     */
    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.nextControlMode();
        if (level != null) {
            setChanged();
            if (!level.isClientSide) {
                balanceWorker.enqueue();
            }
        }
    }

    /**
     * 红石信号变化时由方块调用，使门控的补货输出能即时响应。
     * <p>
     * Called by the block when a neighbor update arrives, so gated restocking reacts
     * to redstone changes immediately.
     */
    public void onNeighborSignalChanged() {
        if (redstoneMode != WarehouseRedstoneMode.IGNORE && level != null && !level.isClientSide) {
            balanceWorker.enqueue();
        }
    }

    /**
     * 判断当前是否允许从城镇仓库补货输出。存回仓库的方向不受红石模式影响。
     * <p>
     * Checks whether restocking output from the town warehouse is currently allowed.
     * Storing items back into the warehouse is never gated.
     */
    private boolean isOutputAllowed() {
        return level != null && redstoneMode.allowsOutput(level.hasNeighborSignal(worldPosition));
    }

    public void setTarget(int slot, @Nullable WarehouseInterfaceTarget target) {
        if (!isSlotValid(slot)) {
            return;
        }
        targets[slot] = target;
        if (level != null) {
            setChanged();
            if (!level.isClientSide) {
                balanceWorker.enqueue();
            }
        }
    }

    public void setTargetFromStack(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            setTarget(slot, WarehouseInterfaceTarget.fromStack(stack));
        }
    }

    public void setTargetAmount(int slot, int amount) {
        WarehouseInterfaceTarget target = getTarget(slot);
        if (target != null) {
            setTarget(slot, target.withAmount(amount));
        }
    }

    private static boolean isSlotValid(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    /**
     * Claims this interface for a warehouse. A still-valid binding owned by a
     * different warehouse is never stolen.
     */
    public boolean tryBind(ITownProviderSerializable<? extends ITownWithBuildings> provider, BlockPos newWarehousePos) {
        if (provider == null || newWarehousePos == null) {
            return false;
        }
        if (isBoundTo(provider, newWarehousePos)) {
            this.townProvider = provider;
            return true;
        }
        if (warehousePos != null && resolveBinding(false).isPresent()) {
            return false;
        }

        this.townProvider = provider;
        this.warehousePos = newWarehousePos.immutable();
        this.connectionStatus = STATUS_UNAVAILABLE;
        if (level != null) {
            setChanged();
        }
        balanceWorker.enqueue();
        return true;
    }

    public void unbindIfBoundTo(ITownProviderSerializable<? extends ITownWithBuildings> provider, BlockPos oldWarehousePos) {
        if (isBoundTo(provider, oldWarehousePos)) {
            clearBinding();
        }
    }

    private boolean isBoundTo(ITownProviderSerializable<? extends ITownWithBuildings> provider, BlockPos candidatePos) {
        return warehousePos != null
                && warehousePos.equals(candidatePos)
                && townProvider != null
                && Objects.equals(townProvider.toNBT(), provider.toNBT());
    }

    private Optional<BindingContext> resolveBinding(boolean clearWhenInvalid) {
        if (townProvider == null || warehousePos == null) {
            return Optional.empty();
        }

        ITownWithBuildings town = townProvider.getTown();
        if (town == null) {
            return invalidBinding(clearWhenInvalid);
        }
        Optional<AbstractTownBuilding> building = town.getTownBuilding(warehousePos);
        if (building.isEmpty() || !(building.get() instanceof WarehouseBuilding warehouse)
                || !warehouse.containsInterface(worldPosition)) {
            return invalidBinding(clearWhenInvalid);
        }

        if (level != null && level.isLoaded(warehousePos)) {
            BlockEntity core = level.getBlockEntity(warehousePos);
            if (!(core instanceof WarehouseBlockEntity)) {
                return invalidBinding(clearWhenInvalid);
            }
        }
        return Optional.of(new BindingContext(town, warehouse));
    }

    private Optional<BindingContext> invalidBinding(boolean clearWhenInvalid) {
        if (clearWhenInvalid) {
            clearBinding();
        }
        return Optional.empty();
    }

    private void clearBinding() {
        boolean changed = townProvider != null || warehousePos != null;
        townProvider = null;
        warehousePos = null;
        connectionStatus = STATUS_UNBOUND;
        if (changed && level != null) {
            setChanged();
        }
    }

    private void validateAndBalance() {
        if (level == null || level.isClientSide) {
            return;
        }
        Optional<BindingContext> binding = resolveBinding(true);
        if (binding.isEmpty()) {
            connectionStatus = STATUS_UNBOUND;
            return;
        }

        BindingContext context = binding.get();
        if (!(context.town() instanceof ITownWithResources resourceTown)
                || !context.warehouse().isBuildingWorkable()) {
            connectionStatus = STATUS_UNAVAILABLE;
            return;
        }

        connectionStatus = STATUS_WORKING;
        balance(resourceTown.getActionExecutorHandler());
    }

    private void balance(IActionExecutorHandler executor) {
        boolean changed = false;

        // Export every wrong item and every item over its configured target first.
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack current = inventory.getStackInSlot(slot);
            if (current.isEmpty()) {
                continue;
            }
            WarehouseInterfaceTarget target = targets[slot];
            int amountToExport;
            if (target == null || !target.matches(current)) {
                amountToExport = current.getCount();
            } else {
                amountToExport = Math.max(0, current.getCount() - target.amount());
            }
            if (amountToExport <= 0) {
                continue;
            }

            ItemStack offered = current.copyWithCount(amountToExport);
            TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(
                    offered, ResourceActionType.ADD, ResourceActionMode.MAXIMIZE);
            TownResourceActionResults.ItemStackActionResult result = executor.execute(action);
            int moved = result.itemStackModified().getCount();
            if (moved > 0) {
                ItemStack remainder = current.copy();
                remainder.shrink(moved);
                setInventoryStackInternal(slot, remainder);
                changed = true;
            }
        }

        // Fill all deficits after exports, allowing items exported above to be reused.
        // Restocking is gated by the redstone control mode; storing back is not.
        boolean outputAllowed = isOutputAllowed();
        for (int slot = 0; slot < SLOT_COUNT && outputAllowed; slot++) {
            WarehouseInterfaceTarget target = targets[slot];
            if (target == null) {
                continue;
            }
            ItemStack current = inventory.getStackInSlot(slot);
            if (!current.isEmpty() && !target.matches(current)) {
                continue;
            }
            int deficit = target.amount() - current.getCount();
            if (deficit <= 0) {
                continue;
            }

            ItemStack requested = target.key().toStack(deficit);
            TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(
                    requested, ResourceActionType.COST, ResourceActionMode.MAXIMIZE);
            TownResourceActionResults.ItemStackActionResult result = executor.execute(action);
            ItemStack extracted = result.itemStackModified();
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack filled = current.isEmpty() ? extracted.copy() : current.copy();
            if (!current.isEmpty()) {
                filled.grow(extracted.getCount());
            }
            setInventoryStackInternal(slot, filled);
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private void setInventoryStackInternal(int slot, ItemStack stack) {
        suppressInventoryCallback = true;
        try {
            inventory.setStackInSlot(slot, stack);
        } finally {
            suppressInventoryCallback = false;
        }
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide) {
            balanceWorker.tick();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            balanceWorker.enqueue();
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void onRemoved() {
        if (level != null && !level.isClientSide) {
            resolveBinding(false).ifPresent(context -> context.warehouse().removeInterface(worldPosition));
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                            worldPosition.getZ() + 0.5, stack.copy());
                    setInventoryStackInternal(slot, ItemStack.EMPTY);
                }
            }
        }
        inventoryCapability.invalidate();
        super.onRemoved();
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        suppressInventoryCallback = true;
        try {
            inventory.deserializeNBT(nbt.getCompound("inventory"));
        } finally {
            suppressInventoryCallback = false;
        }

        redstoneMode = WarehouseRedstoneMode.byOrdinal(nbt.getInt("redstoneMode"), WarehouseRedstoneMode.IGNORE);

        Arrays.fill(targets, null);
        ListTag targetList = nbt.getList("targets", Tag.TAG_COMPOUND);
        for (Tag rawTag : targetList) {
            CompoundTag targetTag = (CompoundTag) rawTag;
            int slot = targetTag.getInt("slot");
            if (isSlotValid(slot) && targetTag.contains("target")) {
                WarehouseInterfaceTarget.CODEC.parse(NbtOps.INSTANCE, targetTag.get("target"))
                        .resultOrPartial(message -> FHMain.LOGGER.warn("Failed to read warehouse interface target: {}", message))
                        .ifPresent(target -> targets[slot] = target);
            }
        }

        townProvider = null;
        warehousePos = null;
        if (nbt.contains("townProvider") && nbt.contains("warehousePos")) {
            ITownProviderSerializable<? extends ITown> rawProvider =
                    ITownProviderSerializable.fromNBT(nbt.getCompound("townProvider"));
            if (rawProvider != null && ITownWithBuildings.class.isAssignableFrom(rawProvider.getTownType())) {
                // The runtime type check above guarantees this provider supplies a town with buildings.
                townProvider = castTownProvider(rawProvider);
                warehousePos = BlockPos.of(nbt.getLong("warehousePos"));
            }
        }
        connectionStatus = warehousePos == null ? STATUS_UNBOUND : STATUS_UNAVAILABLE;
    }

    @SuppressWarnings("unchecked")
    private static ITownProviderSerializable<? extends ITownWithBuildings> castTownProvider(
            ITownProviderSerializable<? extends ITown> provider) {
        return (ITownProviderSerializable<? extends ITownWithBuildings>) provider;
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.put("inventory", inventory.serializeNBT());
        nbt.putInt("redstoneMode", redstoneMode.ordinal());

        ListTag targetList = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            WarehouseInterfaceTarget target = targets[slot];
            if (target == null) {
                continue;
            }
            CompoundTag targetTag = new CompoundTag();
            targetTag.putInt("slot", slot);
            WarehouseInterfaceTarget.CODEC.encodeStart(NbtOps.INSTANCE, target)
                    .resultOrPartial(message -> FHMain.LOGGER.warn("Failed to write warehouse interface target: {}", message))
                    .ifPresent(encoded -> targetTag.put("target", encoded));
            targetList.add(targetTag);
        }
        nbt.put("targets", targetList);

        if (townProvider != null && warehousePos != null) {
            nbt.put("townProvider", townProvider.toNBT());
            nbt.putLong("warehousePos", warehousePos.asLong());
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WarehouseInterfaceMenu(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.warehouse_interface");
    }

    private record BindingContext(ITownWithBuildings town, WarehouseBuilding warehouse) {
    }
}
