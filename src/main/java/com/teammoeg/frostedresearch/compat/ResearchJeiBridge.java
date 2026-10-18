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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.compat;

import com.teammoeg.chorda.CompatModule;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Keeps ordinary research classes loadable when JEI is absent. */
public final class ResearchJeiBridge {
    private ResearchJeiBridge() {
    }

    public static void sync() {
        if (CompatModule.isJeiLoaded()) JEICompat.scheduleSyncJEI();
    }

    public static void addInfo() {
        if (CompatModule.isJeiLoaded()) JEICompat.addInfo();
    }

    public static void showRecipes(ItemStack stack) {
        if (CompatModule.isJeiLoaded()) JEICompat.showJEIFor(stack);
    }

    public static void showCategory(ResourceLocation category) {
        if (CompatModule.isJeiLoaded()) JEICompat.showJEICategory(category);
    }

    public static void appendResearchTooltips(ItemStack stack, List<Component> tooltip) {
        if (!CompatModule.isJeiLoaded()) return;
        JEICompat.research.forEach((locked, descriptions) -> {
            if (ItemStack.isSameItemSameTags(stack, locked)) tooltip.addAll(descriptions.values());
        });
    }
}
