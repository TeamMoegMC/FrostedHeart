/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.client.util;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class GuiUtils {

    public static ResourceLocation makeTextureLocation(String name) {
        return FHMain.rl("textures/gui/" + name + ".png");
    }

    public static StringTextComponent str(String s) {
        return new StringTextComponent(s);
    }

    public static TranslationTextComponent translateGui(String name, Object... args) {
        return new TranslationTextComponent("gui." + FHMain.MODID + "." + name, args);
    }

    public static TranslationTextComponent translateTooltip(String name, Object... args) {
        return new TranslationTextComponent("tooltip." + FHMain.MODID + "." + name, args);
    }

    public static TranslationTextComponent translateMessage(String name, Object... args) {
        return new TranslationTextComponent("message." + FHMain.MODID + "." + name, args);
    }

    public static TranslationTextComponent translateJeiCategory(String name, Object... args) {
        return new TranslationTextComponent("gui.jei.category." + FHMain.MODID + "." + name, args);
    }

    public static TranslationTextComponent translateResearchLevel(String name, Object... args) {
        return new TranslationTextComponent("research.level." + FHMain.MODID + "." + name, args);
    }

    public static TranslationTextComponent translateResearchCategoryName(String name, Object... args) {
        return new TranslationTextComponent("research.category." + FHMain.MODID + "." + name, args);
    }

    public static TranslationTextComponent translateResearchCategoryDesc(String name, Object... args) {
        return new TranslationTextComponent("research.category.desc." + FHMain.MODID + "." + name, args);
    }

    public static ITextComponent translate(String string) {
        return new TranslationTextComponent(string);
    }
}
