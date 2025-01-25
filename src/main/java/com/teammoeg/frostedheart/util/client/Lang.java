/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.client;

import java.util.Locale;

import com.teammoeg.chorda.lang.LangBuilder;
import com.teammoeg.chorda.lang.LangNumberFormat;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

public class Lang {

    /**
     * Use Component.translatable instead
     */
    @Deprecated
    public static MutableComponent translateKey(String string, Object... args) {
        return Component.translatable(string, args);
    }

    @Deprecated
    public static MutableComponent translateGui(String name, Object... args) {
        return gui(name, args).component();
    }

    @Deprecated
    public static MutableComponent translateJeiCategory(String name, Object... args) {
        return jeiCategory(name, args).component();
    }

    @Deprecated
    public static MutableComponent translateMessage(String name, Object... args) {
        return message(name, args).component();
    }

    @Deprecated
    public static MutableComponent translateResearchCategoryDesc(String name, Object... args) {
        return researchCategoryDesc(name, args).component();
    }

    @Deprecated
    public static MutableComponent translateResearchCategoryName(String name, Object... args) {
        return researchCategoryName(name, args).component();
    }

    @Deprecated
    public static MutableComponent translateResearchLevel(String name, Object... args) {
        return researchLevel(name, args).component();
    }

    @Deprecated
    public static MutableComponent translateTooltip(String name, Object... args) {
        return tooltip(name, args).component();
    }

    // New methods from Create

    public static String asId(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static String nonPluralId(String name) {
        String asId = asId(name);
        return asId.endsWith("s") ? asId.substring(0, asId.length() - 1) : asId;
    }

    public static LangBuilder builder() {
        return new LangBuilder(FHMain.MODID);
    }

    public static LangBuilder builder(String namespace) {
        return new LangBuilder(namespace);
    }

    public static LangBuilder blockName(BlockState state) {
        return builder().add(state.getBlock()
                .getName());
    }

    public static LangBuilder itemName(ItemStack stack) {
        return builder().add(stack.getHoverName()
                .copy());
    }

    public static LangBuilder fluidName(FluidStack stack) {
        return builder().add(stack.getDisplayName()
                .copy());
    }
    
    public static LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }

    public static LangBuilder suffix(String langKey, Object... args) {
        return builder().suffix(langKey, args);
    }

    public static LangBuilder prefix(String langKey, Object... args) {
        return builder().prefix(langKey, args);
    }

    public static LangBuilder translate(String prefix, String suffix, Object... args) {
        return builder().translate(prefix, suffix, args);
    }

    public static LangBuilder text(String text) {
        return builder().text(text);
    }

    // wrapper methods
    public static LangBuilder gui(String suffix, Object... args) {
        return translate("gui", suffix, args);
    }

    public static LangBuilder jeiCategory(String suffix, Object... args) {
        return translate("gui.jei.category", suffix, args);
    }

    public static LangBuilder message(String suffix, Object... args) {
        return translate("message", suffix, args);
    }

    public static LangBuilder researchCategoryDesc(String suffix, Object... args) {
        return translate("research.category.desc", suffix, args);
    }

    public static LangBuilder researchCategoryName(String suffix, Object... args) {
        return translate("research.category", suffix, args);
    }

    public static LangBuilder researchLevel(String suffix, Object... args) {
        return translate("research.level", suffix, args);
    }

    public static LangBuilder tooltip(String suffix, Object... args) {
        return translate("tooltip", suffix, args);
    }

    public static LangBuilder waypoint(String suffix, Object... args) {
        return translate("waypoint", suffix, args);
    }

    public static LangBuilder questReward(String suffix, Object... args) {
        return translate("ftbquests.reward", suffix, args);
    }


}
