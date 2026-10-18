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

package com.teammoeg.chorda.compat.jei;

import com.teammoeg.chorda.CompatModule;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class JEICompat {

	public JEICompat() {
	}

    public static void showJEICategory(ResourceLocation rl) {
    	if(CompatModule.isJeiLoaded())
    		JEIPlugin.showJEICategory(rl);
    }

    public static void showJEIFor(ItemStack stack) {
    	if(CompatModule.isJeiLoaded())
    		JEIPlugin.showJEIFor(stack);
    }

    public static void showJEIUsageFor(ItemStack stack) {
    	if(CompatModule.isJeiLoaded())
    		JEIPlugin.showJEIUsageFor(stack);
    }

}
