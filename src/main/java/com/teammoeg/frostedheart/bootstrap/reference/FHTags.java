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

package com.teammoeg.frostedheart.bootstrap.reference;

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceAttribute;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.RegistryAccess;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.tags.ITag;

import java.util.*;

public class FHTags {
	public static final TagKey<ConfiguredFeature<?,?>> BIG_TREE=TagKey.create(Registries.CONFIGURED_FEATURE, FHMain.rl("big_tree"));
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
		CP("caupona"),
		CURIOS("curios"),

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
		ICE_MOVEMENT("movement_modifiers/ice"),
		STONE(NameSpace.FORGE),
		ORES(NameSpace.FORGE),
		SLED_SNOW(),
		SLED_SAND()


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
		public ITag<Block> getTagCollection(){
			return CRegistryHelper.getBlockTag(tag);
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
		COLORED_THERMOS,
		COLORED_ADVANCED_THERMOS,
		THERMOS,
		CHICKEN_FEED,
		POWDERED_SNOW_WALKABLE,
		GARBAGE,
		KNIFE(false),
		BAD_FOOD(false),
		ASH,
		FORBIDDEN_IN_CRATES(NameSpace.IE),
		COW_FEED,
		PIG_FEED,
		CAT_FEED, RABBIT_FEED,
		IMPORTANT_ITEM(false),
		INNER_LINNING(),
		POTASSIUM_RICH,
		NITROGEN_RICH,
		PHOSPHOROUS_RICH,

		// caupona
		WOLFBERRIES(NameSpace.CP),

		// curios
		CURIOS_BACK(NameSpace.CURIOS, "back"),
		CURIOS_CHARM(NameSpace.CURIOS, "charm"),
		CURIOS_HANDS(NameSpace.CURIOS, "hands"),
		SLED_CONTAINER(false)

		;


		public final TagKey<Item> tag;
		public final boolean alwaysDatagen;

		//something about town resource
		/**
		 * 下方的两个Map是用于快速转换ItemResourceKey和TagKey
		 * 所有对应城镇资源的TagKey都在这里自动生成，每一个ItemResourceAttribute对应一个TagKey
		 * 这些自动注册的TagKey都具有"frostedheaft:town_resource_XXX_YYY"的形式
		 * 其中XXX为ItemResourceAttribute中，ItemResourceType名字的小写，YYY为ItemResourceAttribute的level
		 * com.teammoeg.frostedheart.infrastructure.gen.FHRegistrateTag中注册了这些TagKIey。
		 * 同时，若需要在frostedheart环境中为物品添加城镇Tag，也应在FHRegistrateTag中进行。
		 */
		public static final Map<TagKey<Item>, ItemResourceAttribute> MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE = new HashMap<>();
		public static final Map<ItemResourceAttribute, TagKey<Item>> MAP_TOWN_RESOURCE_ATTRIBUTE_TO_TAG = new HashMap<>();
		static{
			NameSpace namespace = NameSpace.MOD;
            for(ItemResourceType type:ItemResourceType.values()){
				for(int i = 0; i <= type.maxLevel; i++){
					ResourceLocation resourceLocation = ResourceLocation.tryBuild(namespace.id, "town_resource_" + type.getKey() + "_" + i);
					ItemResourceAttribute resourceKey = ItemResourceAttribute.of(type, i);
					if (namespace.optionalDefault) {
						MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE.put(optionalTag(ForgeRegistries.ITEMS, resourceLocation), resourceKey);
					} else {
                        if (resourceLocation != null) {
                            MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE.put(ItemTags.create(resourceLocation), resourceKey);
                        } else throw new IllegalArgumentException("ResourceLocation is null! Wrong String might be input when building ResourceLocation!\n" +
								"Input String: " + namespace.id + ":" + "town_resource_" + type.getKey() + "_" + i);
                    }
				}
			}
			MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE.forEach((tag, resourceAttribute) -> MAP_TOWN_RESOURCE_ATTRIBUTE_TO_TAG.put(resourceAttribute, tag));
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
		Items(boolean optional, boolean alwaysDatagen) {
			this(NameSpace.MOD, null, optional, alwaysDatagen);
		}
		Items(boolean alwaysDatagen) {
			this(NameSpace.MOD, null, NameSpace.MOD.optionalDefault, alwaysDatagen);
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
		/**
		 * Convenience method to get all items in a tag
		 * */
		public ITag<Item> getTagCollection(){
			return CRegistryHelper.getItemTag(tag);
		}

		private static void init() {}
	}

	public enum FHEntityTags {

		SLED_PULLERS(false)
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

		FHEntityTags(boolean alwaysDatagen) {
			this(NameSpace.MOD, null, NameSpace.MOD.optionalDefault, alwaysDatagen);
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
		DRINK(false),
		HIDDEN_DRINK(false),
		FLAMMABLE_FLUID(false),
		WOODEN_CUP_DRINK(false),
		IRON_CUP_DRINK(false)
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
		Fluids(boolean optional, boolean alwaysDatagen) {
			this(NameSpace.MOD, null, optional, alwaysDatagen);
		}
		Fluids(boolean alwaysDatagen) {
			this(NameSpace.MOD, null, NameSpace.MOD.optionalDefault, alwaysDatagen);
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
		public ITag<Fluid> getTagCollection(){
			return CRegistryHelper.getFluidTag(tag);
		}
		private static void init() {}
	}
	public enum Biomes {
		IS_ORE_VEIN(false),
		IS_CAVE(false)
		;

		public final TagKey<Biome> tag;
		public final boolean alwaysDatagen;

		Biomes() {
			this(NameSpace.MOD);
		}

		Biomes(String path) {
			this(NameSpace.MOD, path);
		}

		Biomes(NameSpace namespace) {
			this(namespace, namespace.alwaysDatagenDefault);
		}

		Biomes(NameSpace namespace, String path) {
			this(namespace, path, namespace.alwaysDatagenDefault);
		}
		Biomes(boolean alwaysDatagen) {
			this(NameSpace.MOD, null, alwaysDatagen);
		}
		Biomes(NameSpace namespace, boolean alwaysDatagen) {
			this(namespace, null, alwaysDatagen);
		}

		Biomes(NameSpace namespace, String path,  boolean alwaysDatagen) {
			ResourceLocation id = new ResourceLocation(namespace.id, path == null ? Lang.asId(name()) : path);
			tag=TagKey.create(Registries.BIOME,id);
			this.alwaysDatagen = alwaysDatagen;
		}

		public boolean matches(Holder<Biome> biome) {
			return biome.is(tag);
		}


		public TagKey<Biome> get() {
			return tag;
		}
		public Named<Biome> getTagCollection(RegistryAccess ra){
			return ra.registry(ForgeRegistries.Keys.BIOMES).get().getOrCreateTag(tag);
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
