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

/** The bounded set of visual assemblies, independent of the optional Tetra implementation. */
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

    /** Checks only the item identity, without resolving Tetra slots or variants. */
    public static boolean isDrill(ItemStack stack) {
        return stack.getItem() instanceof ItemView;
    }

    /** A synchronized replacement stack can still be the same drill for the work pose. */
    public static boolean sameVisualTool(ItemStack previous, ItemStack current) {
        return previous == current || (previous.getItem() instanceof ItemView drill
                && previous.getItem() == current.getItem() && drill.sameVisualIdentity(previous, current));
    }

    /** Snapshot only the stock item-model inputs for a charger, never the actual inventory stack. */
    @Nullable
    public static CompoundTag dockAppearance(ItemStack stack) {
        return stack.getItem() instanceof ItemView drill ? drill.dockAppearance(stack) : null;
    }

    /** Returns the installed assembly, or null for items other than S04. Empty slots use the default visual parts. */
    @Nullable
    public static StrataDrillAssembly read(ItemStack stack) {
        return stack.getItem() instanceof ItemView drill ? drill.resolveAssembly(stack, false) : null;
    }

    /** Resolve both slots once, rejecting missing modules and tools out of durability. */
    @Nullable
    static StrataDrillAssembly operable(ItemStack stack) {
        return stack.getItem() instanceof ItemView drill ? drill.resolveAssembly(stack, true) : null;
    }

    /** Migrate an older unidentified drill only when a real refill has changed its state. */
    static void identifyAfterRefill(ItemStack stack) {
        if (stack.getItem() instanceof ItemView drill) drill.ensureIdentity(stack);
    }

    static StrataDrillAssembly of(boolean steam, boolean wheel) {
        if (steam) {
            return wheel ? STEAM_WHEEL : STEAM_SPIRAL;
        }
        return wheel ? DIESEL_WHEEL : DIESEL_SPIRAL;
    }

    /** Keeps the common API loadable on installations without Tetra. */
    interface ItemView {
        @Nullable
        StrataDrillAssembly resolveAssembly(ItemStack stack, boolean requireOperable);

        CompoundTag dockAppearance(ItemStack stack);

        void ensureIdentity(ItemStack stack);

        boolean sameVisualIdentity(ItemStack previous, ItemStack current);
    }
}
