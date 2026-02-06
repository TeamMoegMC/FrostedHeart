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

package com.teammoeg.frostedresearch;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Lang {

	public Lang() {

	}
	public static MutableComponent translate(String prefix,String suffix,Object...args) {
		return Component.translatable(prefix+"."+FRMain.MODID+"."+suffix, args);
	}
    public static MutableComponent researchCategoryDesc(String suffix, Object... args) {
        return translate("research.category.desc", suffix, args);
    }

    public static MutableComponent researchCategoryName(String suffix, Object... args) {
        return translate("research.category", suffix, args);
    }
    public static MutableComponent translateKey(String key,Object...args) {
		return Component.translatable(key, args);
	}
    
    public static MutableComponent translateGui(String name, Object... args) {
        return translate("gui",name, args);
    }
    public static MutableComponent researchLevel(String suffix, Object... args) {
        return translate("research.level", suffix, args);
    }
    public static MutableComponent translateResearchCategoryDesc(String name, Object... args) {
        return researchCategoryDesc(name, args);
    }
    public static MutableComponent translateMessage(String name, Object... args) {
        return translate("message",name, args);
    }
    public static MutableComponent translateTooltip(String name, Object... args) {
        return translate("tooltip",name, args);
    }

    public static MutableComponent translateResearchCategoryName(String name, Object... args) {
        return researchCategoryName(name, args);
    }

    public static MutableComponent translateResearchLevel(String name, Object... args) {
        return researchLevel(name, args);
    }
}
