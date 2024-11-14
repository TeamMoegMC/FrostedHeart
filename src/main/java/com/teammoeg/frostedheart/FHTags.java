/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class FHTags {
	public static final class Blocks {
//        public static final ITag.INamedTag<Block> ALWAYS_BREAKABLE = create("always_breakable");
		public static final TagKey<Block> TOWN_DECORATIONS = tag("town/decorations");
		public static final TagKey<Block> TOWN_WALLS = tag("town/walls");
		public static final TagKey<Block> CONDENSED_ORES = tag("condensed_ores");
		public static final TagKey<Block> PERMAFROST = tag("permafrost");
		public static final TagKey<Block> TOWN_BLOCKS = tag("town/blocks");
		public static final TagKey<Block> METAL_MACHINES = tag("machines/metal");
		public static final TagKey<Block> WOODEN_MACHINES = tag("machines/wooden");
		public static final TagKey<Block> SOIL = tag("soil");
		public static final TagKey<Block> SNOW_MOVEMENT = tag("movement_modifiers/snow");
		public static final TagKey<Block> ICE_MOVEMENT = tag("movement_modifiers/ice");


		private static TagKey<Block> tag(String name) {
			return BlockTags.create(new ResourceLocation(FHMain.MODID, name));
		}
	}

	public static final class Items {
//      public static final ITag.INamedTag<Block> ALWAYS_BREAKABLE = create("always_breakable");
		public static final TagKey<Item> RAW_FOOD = tag("raw_food");
		public static final TagKey<Item> CONDENSED_BALLS = tag("condensed_balls");
		public static final TagKey<Item> PERMAFROST = tag("permafrost");
		public static final TagKey<Item> IGNITION_MATERIAL = tag("ignition_material");
		public static final TagKey<Item> IGNITION_METAL = tag("ignition_metal");
		public static final TagKey<Item> REFUGEE_NEEDS = tag("refugee_needs");

		private static TagKey<Item> tag(String name) {
			return ItemTags.create(new ResourceLocation(FHMain.MODID, name));
		}
	}

	public static final class Fluids{

		public static final TagKey<Fluid> DRINK = tag("drink");

		private static TagKey<Fluid> tag(String name) {
			return FluidTags.create(new ResourceLocation(FHMain.MODID, name));
		}

	}

}
