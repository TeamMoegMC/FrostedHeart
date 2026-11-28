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

package com.teammoeg.frostedheart.content.stratadrill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

/** The S04's own save format. Reading a stack, including a raw command stack, never initialises it. */
public final class StrataDrillData {
    static final String ROOT = "frostedheart:stratadrill";
    static final String DEFAULT_CUTTER = "iron";
    private static final int VERSION = 1;

    private StrataDrillData() {}

    /** Absent/unsupported data reads as an empty steam spiral, not as old development-format parts. */
    @Nullable
    static CompoundTag read(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data != null && data.getInt("v") == VERSION ? data : null;
    }

    /** Materialise v1 only for a real write; unrelated vanilla/mod tags remain untouched. */
    static CompoundTag edit(ItemStack stack) {
        CompoundTag data = read(stack);
        if (data == null) {
            data = new CompoundTag();
            data.putInt("v", VERSION);
            data.putBoolean("wheel", false);
            data.putBoolean("diesel", false);
            data.putString("cutter", DEFAULT_CUTTER);
            stack.addTagElement(ROOT, data);
        }
        return data;
    }

    static StrataDrillAssembly assembly(@Nullable CompoundTag data) {
        return data == null ? StrataDrillAssembly.STEAM_SPIRAL
                : StrataDrillAssembly.of(!data.getBoolean("diesel"), data.getBoolean("wheel"));
    }

    /** Change only installed hardware, never fuel, cutter wear or identity. */
    public static void setAssembly(ItemStack stack, StrataDrillAssembly assembly) {
        CompoundTag data = edit(stack);
        data.putBoolean("wheel", assembly.wheel());
        data.putBoolean("diesel", !assembly.steam());
    }

    /** Return the stored material key, including a temporarily absent datapack material. */
    public static String cutterId(ItemStack stack) {
        return storedCutter(read(stack));
    }

    static String storedCutter(@Nullable CompoundTag data) {
        if (data == null) return DEFAULT_CUTTER;
        String cutter = data.getString("cutter");
        return cutter.isEmpty() ? DEFAULT_CUTTER : cutter;
    }

    /**
     * Select a cutter, retaining wear up to its terminal point. Only an actual material change
     * binds the raw wear to this capacity; the workbench owns the single proportional conversion.
     */
    public static void setCutter(ItemStack stack, String cutter) {
        CompoundTag data = edit(stack);
        String next = cutter.isEmpty() ? DEFAULT_CUTTER : cutter;
        boolean changed = !next.equals(storedCutter(data));
        data.putString("cutter", next);
        int wear = wear(data);
        if (changed && wear > 0) {
            int limit = StrataDrillMaterials.get(next).durability() - 1;
            if (wear > limit) data.putInt("wear", limit);
        }
    }

    static int wear(@Nullable CompoundTag data) {
        return data == null ? 0 : Math.max(0, data.getInt("wear"));
    }

    static void setWear(ItemStack stack, int wear) {
        if (wear > 0) {
            edit(stack).putInt("wear", wear);
        } else {
            CompoundTag data = read(stack);
            if (data != null) data.remove("wear");
        }
    }

    /** Synchronized copies retain their identity; uninitialised stacks have none. */
    public static String identity(ItemStack stack) {
        CompoundTag data = read(stack);
        return data == null ? "" : data.getString("id");
    }

    /** Assign an ID at creation or on a successful refill; never churn it while the drill works. */
    public static void ensureIdentity(ItemStack stack) {
        if (identity(stack).isEmpty()) edit(stack).putString("id", UUID.randomUUID().toString());
    }
}
