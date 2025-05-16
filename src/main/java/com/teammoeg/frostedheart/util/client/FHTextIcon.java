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

import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Text icon registry.
 */
public class FHTextIcon {
	
	/** The ResourceLocation for our own font. */
	private static final ResourceLocation iconFont= FHMain.rl("default");
	
	/**
	 * TextIconType Registry Object.
	 *
	 * @param code the code point
	 */
	public static record TextIconType(String code){
		
		/**
		 * Gets the sole icon component.
		 * Must not add siblings or append to it as the style would be append to all siblings
		 * @return the icon
		 */
		public MutableComponent getIcon() {
			return Components.str(code).withStyle(t->t.withFont(iconFont));
		}
		
		/**
		 * Returns new component with icon before the specific component .
		 *
		 * @param which the original component
		 * @return the new component
		 */
		public MutableComponent appendBefore(Component which) {
			return Components.str("").append(getIcon()).append(which);
			
		}
		
		/**
		 * Returns new component with icon after the specific component .
		 *
		 * @param which the original component
		 * @return the new component
		 */
		public MutableComponent appendAfter(Component which) {
			return Components.str("").append(which).append(getIcon());
		}
	}
	
	/** thermometer icon . */
	public static final TextIconType thermometer=new TextIconType("\uF500");
	public static final TextIconType SOIL_THERMOMETER = new TextIconType("\uF520");
	public static final TextIconType LIGHT = new TextIconType("\uF521");
	public static Style applyFont(Style style) {
		return style.withFont(iconFont);
	}
}
