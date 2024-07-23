/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import java.util.Collection;
import java.util.Map;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class TranslateUtils {

    public static ResourceLocation makeTextureLocation(String name) {
        return FHMain.rl("textures/gui/" + name + ".png");
    }

    public static TextComponent str(String s) {
        return new TextComponent(s);
    }

    /**
     * Convert a collection to a string text component
     * <p></p>
     * Lists all elements in the collection, separated by new lines
     * Uses the toString method of each element
     * @param collection the collection
     * @return the string text component
     * @param <V> the type of the collection
     */
    public static <V> TextComponent str(Collection<V> collection) {

        StringBuilder sb = new StringBuilder();
        for (V v : collection) {
            sb.append(v.toString()).append("\n");
        }
        // remove the last newline if the string is not empty
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return new TextComponent(sb.toString());
    }

    /**
     * Convert a Map to a string text component
     * <p></p>
     * Lists all elements in the map, separated by new lines
     * For each entry, use the toString method of the key and value,
     * separated by a colon and a space
     * @param map the map
     * @return the string text component
     * @param <K> the type of the keys
     * @param <V> the type of the values
     */
    public static <K, V> TextComponent str(Map<K, V> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sb.append(entry.getKey().toString()).append(": ").append(entry.getValue().toString()).append("\n");
        }
        return new TextComponent(sb.toString());
    }

    public static TranslatableComponent translate(String string, Object... args) {
        return new TranslatableComponent(string, args);
    }

    public static TranslatableComponent translateGui(String name, Object... args) {
        return translate("gui." + FHMain.MODID + "." + name, args);
    }

    public static TranslatableComponent translateJeiCategory(String name, Object... args) {
        return translate("gui.jei.category." + FHMain.MODID + "." + name, args);
    }

    public static TranslatableComponent translateMessage(String name, Object... args) {
        return translate("message." + FHMain.MODID + "." + name, args);
    }

    public static TranslatableComponent translateResearchCategoryDesc(String name, Object... args) {
        return translate("research.category.desc." + FHMain.MODID + "." + name, args);
    }

    public static TranslatableComponent translateResearchCategoryName(String name, Object... args) {
        return translate("research.category." + FHMain.MODID + "." + name, args);
    }

    public static TranslatableComponent translateResearchLevel(String name, Object... args) {
        return translate("research.level." + FHMain.MODID + "." + name, args);
    }

    public static TranslatableComponent translateTooltip(String name, Object... args) {
        return translate("tooltip." + FHMain.MODID + "." + name, args);
    }
}
