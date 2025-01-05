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

import com.simibubi.create.foundation.utility.Lang;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceKey;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FHTags {

	public static <T> TagKey<T> optionalTag(IForgeRegistry<T> registry,
											ResourceLocation id) {
		return registry.tags()
				.createOptionalTagKey(id, Collections.emptySet());
	}

	public static <T> TagKey<T> forgeTag(IForgeRegistry<T> registry, String path) {
		return optionalTag(registry, new ResourceLocation("forge", path));
	}

	public static TagKey<Block> forgeBlockTag(String path) {
		return forgeTag(ForgeRegistries.BLOCKS, path);
	}

	public static TagKey<Item> forgeItemTag(String path) {
		return forgeTag(ForgeRegistries.ITEMS, path);
	}

	public static TagKey<Fluid> forgeFluidTag(String path) {
		return forgeTag(ForgeRegistries.FLUIDS, path);
	}

	public enum NameSpace {
		MOD(FHMain.MODID, false, true),
		FORGE("forge"),
		MC("minecraft"),
		CREATE("create"),
		IE("immersiveengineering"),
		TETRA("tetra"),
		SP("steampowered"),
		CP("caupona")

		;

		public final String id;
		public final boolean optionalDefault;
		public final boolean alwaysDatagenDefault;

		NameSpace(String id) {
			this(id, true, false);
		}

		NameSpace(String id, boolean optionalDefault, boolean alwaysDatagenDefault) {
			this.id = id;
			this.optionalDefault = optionalDefault;
			this.alwaysDatagenDefault = alwaysDatagenDefault;
		}

	}

	/**
	 * If no path is provided, the tag will be created with the name of the enum constant.
	 */
	public enum Blocks {
		TOWN_DECORATIONS("town/decorations"),
		TOWN_WALLS("town/walls"),
		CONDENSED_ORES,
		SLUDGE,
		PERMAFROST,
		TOWN_BLOCKS("town/blocks"),
		METAL_MACHINES("machines/metal"),
		WOODEN_MACHINES("machines/wooden"),
		SOIL,
		SNOW_MOVEMENT("movement_modifiers/snow"),
		ICE_MOVEMENT("movement_modifiers/ice")


		;

		public final TagKey<Block> tag;
		public final boolean alwaysDatagen;

		Blocks() {
			this(NameSpace.MOD);
		}

		Blocks(String path) {
			this(NameSpace.MOD, path);
		}

		Blocks(NameSpace namespace) {
			this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		Blocks(NameSpace namespace, String path) {
			this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		Blocks(NameSpace namespace, boolean optional, boolean alwaysDatagen) {
			this(namespace, null, optional, alwaysDatagen);
		}

		Blocks(NameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
			ResourceLocation id = new ResourceLocation(namespace.id, path == null ? Lang.asId(name()) : path);
			if (optional) {
				tag = optionalTag(ForgeRegistries.BLOCKS, id);
			} else {
				tag = BlockTags.create(id);
			}
			this.alwaysDatagen = alwaysDatagen;
		}

		public boolean matches(Block block) {
			return block.builtInRegistryHolder()
					.is(tag);
		}

		public boolean matches(ItemStack stack) {
			return stack != null && stack.getItem() instanceof BlockItem blockItem && matches(blockItem.getBlock());
		}

		public boolean matches(BlockState state) {
			return state.is(tag);
		}

		public TagKey<Block> get() {
			return tag;
		}

		private static void init() {}
	}

	public enum Items {
		RAW_FOOD,
		CONDENSED_BALLS,
		SLURRY,
		PERMAFROST,
		IGNITION_MATERIAL,
		IGNITION_METAL,
		REFUGEE_NEEDS,
		DRY_FOOD,
		INSULATED_FOOD,

		;

		public final TagKey<Item> tag;
		public final boolean alwaysDatagen;

		public static final Map<TagKey<Item>, ItemResourceKey> TOWN_RESOURCE_TAGS = new HashMap<>();
		static{
			NameSpace namespace = NameSpace.MOD;
			//TownResourceType有一个maxLevel的属性，找到最大的maxLevel，以生成适当数量的tag来表示level
			AtomicInteger max_maxLevel = new AtomicInteger(0);
			for(ItemResourceType type:ItemResourceType.values()){
				for(int i = 0; i <= type.maxLevel; i++){
					ResourceLocation resourceLocation = new ResourceLocation(namespace.id, "town_resource_" + type.getKey() + "_" + i);
					ItemResourceKey resourceKey = ItemResourceKey.of(type, i);
					if (namespace.optionalDefault) {
						TOWN_RESOURCE_TAGS.put(optionalTag(ForgeRegistries.ITEMS, resourceLocation), resourceKey);
					} else {
						TOWN_RESOURCE_TAGS.put(ItemTags.create(resourceLocation), resourceKey);
					}
				}
			}
		}

		Items() {
			this(NameSpace.MOD);
		}

		Items(NameSpace namespace) {
			this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		Items(NameSpace namespace, String path) {
			this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		Items(NameSpace namespace, boolean optional, boolean alwaysDatagen) {
			this(namespace, null, optional, alwaysDatagen);
		}

		Items(NameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
			ResourceLocation id = new ResourceLocation(namespace.id, path == null ? Lang.asId(name()) : path);
			if (optional) {
				tag = optionalTag(ForgeRegistries.ITEMS, id);
			} else {
				tag = ItemTags.create(id);
			}
			this.alwaysDatagen = alwaysDatagen;
		}

		@SuppressWarnings("deprecation")
		public boolean matches(Item item) {
			return item.builtInRegistryHolder()
					.is(tag);
		}

		public boolean matches(ItemStack stack) {
			return stack.is(tag);
		}

		private static void init() {}
	}

	public enum FHEntityTags {

		// Nothing yet
		// NANITES,

		;

		public final TagKey<EntityType<?>> tag;
		public final boolean alwaysDatagen;

		FHEntityTags() {
			this(NameSpace.MOD);
		}

		FHEntityTags(NameSpace namespace) {
			this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		FHEntityTags(NameSpace namespace, String path) {
			this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		FHEntityTags(NameSpace namespace, boolean optional, boolean alwaysDatagen) {
			this(namespace, null, optional, alwaysDatagen);
		}

		FHEntityTags(NameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
			ResourceLocation id = new ResourceLocation(namespace.id, path == null ? Lang.asId(name()) : path);
			if (optional) {
				tag = optionalTag(ForgeRegistries.ENTITY_TYPES, id);
			} else {
				tag = TagKey.create(Registries.ENTITY_TYPE, id);
			}
			this.alwaysDatagen = alwaysDatagen;
		}

		public boolean matches(EntityType<?> type) {
			return type.is(tag);
		}

		public boolean matches(Entity entity) {
			return matches(entity.getType());
		}

		private static void init() {}

	}

	public enum Fluids {
		DRINK
		;

		public final TagKey<Fluid> tag;
		public final boolean alwaysDatagen;

		Fluids() {
			this(NameSpace.MOD);
		}

		Fluids(String path) {
			this(NameSpace.MOD, path);
		}

		Fluids(NameSpace namespace) {
			this(namespace, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		Fluids(NameSpace namespace, String path) {
			this(namespace, path, namespace.optionalDefault, namespace.alwaysDatagenDefault);
		}

		Fluids(NameSpace namespace, boolean optional, boolean alwaysDatagen) {
			this(namespace, null, optional, alwaysDatagen);
		}

		Fluids(NameSpace namespace, String path, boolean optional, boolean alwaysDatagen) {
			ResourceLocation id = new ResourceLocation(namespace.id, path == null ? Lang.asId(name()) : path);
			if (optional) {
				tag = optionalTag(ForgeRegistries.FLUIDS, id);
			} else {
				tag = FluidTags.create(id);
			}
			this.alwaysDatagen = alwaysDatagen;
		}

		public boolean matches(Fluid fluid) {
			return fluid.builtInRegistryHolder()
					.is(tag);
		}

		public boolean matches(FluidState state) {
			return state.is(tag);
		}

		public TagKey<Fluid> get() {
			return tag;
		}

		private static void init() {}
	}

	public static void init() {
		FHTags.Blocks.init();
		FHTags.Fluids.init();
		FHTags.Items.init();
		FHTags.FHEntityTags.init();
	}

}
