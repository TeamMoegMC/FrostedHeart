/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Nine-entry whitelist/blacklist filter; empty configuration always allows all items. */
public final class P2PItemFilter {
    public static final int SLOT_COUNT = 9;

    private final NonNullList<ItemStack> entries = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean whitelist = true;
    private boolean fuzzy;

    public boolean matches(ItemStack candidate) {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        boolean configured = false;
        boolean matched = false;
        for (ItemStack entry : entries) {
            if (entry.isEmpty()) {
                continue;
            }
            configured = true;
            if (fuzzy ? entry.is(candidate.getItem())
                    : ItemStack.isSameItemSameTags(entry, candidate)) {
                matched = true;
                break;
            }
        }
        return !configured || whitelist == matched;
    }

    public ItemStack getEntry(int slot) {
        return valid(slot) ? entries.get(slot).copy() : ItemStack.EMPTY;
    }

    public void setEntry(int slot, ItemStack stack) {
        if (valid(slot)) {
            entries.set(slot, stack == null || stack.isEmpty()
                    ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public void setFuzzy(boolean fuzzy) {
        this.fuzzy = fuzzy;
    }

    public CompoundTag serializeNBT() {
        CompoundTag result = new CompoundTag();
        result.putBoolean("whitelist", whitelist);
        result.putBoolean("fuzzy", fuzzy);
        ListTag list = new ListTag();
        for (int slot = 0; slot < entries.size(); slot++) {
            ItemStack entry = entries.get(slot);
            if (!entry.isEmpty()) {
                CompoundTag encoded = entry.save(new CompoundTag());
                encoded.putInt("filterSlot", slot);
                list.add(encoded);
            }
        }
        result.put("entries", list);
        return result;
    }

    public void deserializeNBT(CompoundTag tag) {
        entries.replaceAll(ignored -> ItemStack.EMPTY);
        whitelist = !tag.contains("whitelist") || tag.getBoolean("whitelist");
        fuzzy = tag.getBoolean("fuzzy");
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (Tag value : list) {
            CompoundTag encoded = (CompoundTag) value;
            int slot = encoded.getInt("filterSlot");
            if (valid(slot)) {
                entries.set(slot, ItemStack.of(encoded).copyWithCount(1));
            }
        }
    }

    private static boolean valid(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }
}
