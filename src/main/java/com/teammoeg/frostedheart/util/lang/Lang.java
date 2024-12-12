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

package com.teammoeg.frostedheart.util.lang;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

public class Lang {

    public static ResourceLocation makeTextureLocation(String name) {
        return FHMain.rl("textures/gui/" + name + ".png");
    }

    public static MutableComponent str(String s) {
    	if(s==null||s.isEmpty()) {
    		return Component.empty();
    	}
        return MutableComponent.create(new LiteralContents(s));
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
    public static <V> MutableComponent str(Collection<V> collection) {

        StringBuilder sb = new StringBuilder();
        for (V v : collection) {
            sb.append(v.toString()).append("\n");
        }
        // remove the last newline if the string is not empty
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return str(sb.toString());
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
    public static <K, V> MutableComponent str(Map<K, V> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sb.append(entry.getKey().toString()).append(": ").append(entry.getValue().toString()).append("\n");
        }
        return str(sb.toString());
    }

    public static MutableComponent translateKey(String string, Object... args) {
        return Component.translatable(string, args);
    }

    public static MutableComponent translateOrElseStr(String string) {
        return I18n.exists(string) ? translateKey(string) : str(string);
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

    @Deprecated
    public static MutableComponent translateTips(String name, Object... args) {
        return tips(name, args).component();
    }

    @Deprecated
    public static MutableComponent translateWaypoint(String name, Object... args) {
        return waypoint(name, args).component();
    }

    public static String rawQuestReward(String name, Object... args) {
        return "ftbquests.reward." + FHMain.MODID + "." + name;
    }

	public static Component empty() {
		return MutableComponent.create(LiteralContents.EMPTY);
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

    public static Object[] resolveBuilders(Object[] args) {
        for (int i = 0; i < args.length; i++)
            if (args[i]instanceof LangBuilder cb)
                args[i] = cb.component();
        return args;
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

    public static LangBuilder tips(String suffix, Object... args) {
        return translate("tips", suffix, args);
    }

    public static LangBuilder waypoint(String suffix, Object... args) {
        return translate("waypoint", suffix, args);
    }

    public static LangBuilder questReward(String suffix, Object... args) {
        return translate("ftbquests.reward", suffix, args);
    }


}
