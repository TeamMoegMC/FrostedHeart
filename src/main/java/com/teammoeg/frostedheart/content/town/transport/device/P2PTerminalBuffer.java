/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Eight real slots with separate automation and P2P views. */
public final class P2PTerminalBuffer {
    public static final int PENDING_SLOTS = 4;
    public static final int RECEIVED_SLOTS = 4;
    public static final int TOTAL_SLOTS = PENDING_SLOTS + RECEIVED_SLOTS;

    private final Runnable changeListener;
    private boolean suppressCallback;
    private boolean pendingLocked;
    private boolean receivedLocked;
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!suppressCallback) {
                changeListener.run();
            }
        }
    };
    private final IItemHandler externalView = new ExternalView();
    private final IItemHandler p2pSourceView = new RangeView(0, PENDING_SLOTS, true, false);
    private final IItemHandler p2pTargetView = new RangeView(PENDING_SLOTS, RECEIVED_SLOTS, false, true);

    public P2PTerminalBuffer(Runnable changeListener) {
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public IItemHandler externalView() {
        return externalView;
    }

    public IItemHandler p2pSourceView() {
        return p2pSourceView;
    }

    public IItemHandler p2pTargetView() {
        return p2pTargetView;
    }

    public boolean isPendingLocked() {
        return pendingLocked;
    }

    public boolean isReceivedLocked() {
        return receivedLocked;
    }

    public boolean setLocks(boolean pendingLocked, boolean receivedLocked) {
        if (this.pendingLocked == pendingLocked && this.receivedLocked == receivedLocked) {
            return false;
        }
        this.pendingLocked = pendingLocked;
        this.receivedLocked = receivedLocked;
        changeListener.run();
        return true;
    }

    public CompoundTag serializeNBT() {
        return inventory.serializeNBT();
    }

    public void deserializeNBT(CompoundTag tag) {
        suppressCallback = true;
        try {
            inventory.deserializeNBT(tag);
        } finally {
            suppressCallback = false;
        }
    }

    private final class ExternalView implements IItemHandler {
        @Override
        public int getSlots() {
            return TOTAL_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return valid(slot) ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!valid(slot) || slot >= PENDING_SLOTS || pendingLocked) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!valid(slot) || slot < PENDING_SLOTS || receivedLocked) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return valid(slot) ? inventory.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return valid(slot) && slot < PENDING_SLOTS && !pendingLocked
                    && inventory.isItemValid(slot, stack);
        }
    }

    private final class RangeView implements IItemHandler {
        private final int offset;
        private final int slots;
        private final boolean source;
        private final boolean target;

        private RangeView(int offset, int slots, boolean source, boolean target) {
            this.offset = offset;
            this.slots = slots;
            this.source = source;
            this.target = target;
        }

        @Override
        public int getSlots() {
            return slots;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return inRange(slot) ? inventory.getStackInSlot(offset + slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!inRange(slot)) {
                return stack;
            }
            if (source) {
                // Source insertion is reserved for immediate rollback after a partial commit.
                return inventory.insertItem(offset + slot, stack, simulate);
            }
            if (!target || receivedLocked) {
                return stack;
            }
            return inventory.insertItem(offset + slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!inRange(slot) || !source || pendingLocked) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(offset + slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inRange(slot) ? inventory.getSlotLimit(offset + slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (!inRange(slot)) {
                return false;
            }
            if (source) {
                return inventory.isItemValid(offset + slot, stack);
            }
            return target && !receivedLocked && inventory.isItemValid(offset + slot, stack);
        }

        private boolean inRange(int slot) {
            return slot >= 0 && slot < slots;
        }
    }

    private static boolean valid(int slot) {
        return slot >= 0 && slot < TOTAL_SLOTS;
    }
}
