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
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** The four S04 assemblies used by gameplay, rendering and the optional workbench. */
public enum StrataDrillAssembly {
    STEAM_SPIRAL(true, false),
    STEAM_WHEEL(true, true),
    DIESEL_SPIRAL(false, false),
    DIESEL_WHEEL(false, true);

    private final boolean steam;
    private final boolean wheel;

    StrataDrillAssembly(boolean steam, boolean wheel) {
        this.steam = steam;
        this.wheel = wheel;
    }

    public boolean steam() {
        return steam;
    }

    public boolean wheel() {
        return wheel;
    }

    /** Checks only the item type, without reading its parts, cutter or fuel. */
    public static boolean isDrill(ItemStack stack) {
        return stack.getItem() instanceof StrataDrillItem;
    }

    /** A synchronized replacement stack can still be the same drill for the work pose. */
    public static boolean sameVisualTool(ItemStack previous, ItemStack current) {
        if (previous == current) return true;
        if (!isDrill(previous) || previous.getItem() != current.getItem()) return false;
        String id = StrataDrillData.identity(previous);
        return !id.isEmpty() && id.equals(StrataDrillData.identity(current));
    }

    /** Snapshot only the stock item-model inputs for a charger, never the actual inventory stack. */
    @Nullable
    public static CompoundTag dockAppearance(ItemStack stack) {
        StrataDrillAssembly assembly = read(stack);
        if (assembly == null) return null;
        ItemStack display = new ItemStack(stack.getItem());
        StrataDrillData.setAssembly(display, assembly);
        StrataDrillData.setCutter(display, StrataDrillData.cutterId(stack));
        if (stack.hasFoil()) {
            // Vanilla's foil layer only needs a nonempty enchantment list, not the actual enchantments.
            ListTag foil = new ListTag();
            foil.add(new CompoundTag());
            display.addTagElement(ItemStack.TAG_ENCH, foil);
        }
        return display.save(new CompoundTag());
    }

    /** Returns the installed assembly even when dry or worn out; raw drills use steam and a spiral. */
    @Nullable
    public static StrataDrillAssembly read(ItemStack stack) {
        return isDrill(stack) ? StrataDrillData.assembly(StrataDrillData.read(stack)) : null;
    }

    /** Resolve the installed parts, rejecting a worn-out cutter but not empty fuel tanks. */
    @Nullable
    static StrataDrillAssembly operable(ItemStack stack) {
        if (!isDrill(stack)) return null;
        CompoundTag data = StrataDrillData.read(stack);
        return StrataDrillItem.cutterCanRun(data) ? StrataDrillData.assembly(data) : null;
    }

    /** Give a raw command drill its identity only when a real refill has changed its state. */
    static void identifyAfterRefill(ItemStack stack) {
        if (isDrill(stack)) StrataDrillData.ensureIdentity(stack);
    }

    /** Select the shared assembly without creating a per-stack part object. */
    public static StrataDrillAssembly of(boolean steam, boolean wheel) {
        if (steam) {
            return wheel ? STEAM_WHEEL : STEAM_SPIRAL;
        }
        return wheel ? DIESEL_WHEEL : DIESEL_SPIRAL;
    }

}
