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
        if (CompatModule.isJeiLoaded()) JEICompat.syncJEI();
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
