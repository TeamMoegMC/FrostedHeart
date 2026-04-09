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

package com.teammoeg.chorda.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * 注册表访问辅助类，提供便捷的方法来获取和查询Minecraft注册表中的各类对象。
 * 支持方块、物品、流体、生物群系、附魔、实体类型、村民职业等的注册表操作。
 * <p>
 * Registry access helper class providing convenient methods to get and query
 * various objects in Minecraft registries. Supports blocks, items, fluids, biomes,
 * enchantments, entity types, villager professions, and more.
 */
public class CRegistryHelper {
	/**
	 * 获取方块的注册路径。
	 * <p>
	 * Get the registry path of a block.
	 *
	 * @param v 方块 / the block
	 * @return 注册路径 / the registry path
	 */
	public static String getPath(Block v) {
		return getRegistryName(v).getPath();
	}

	/**
	 * 获取物品的注册路径。
	 * <p>
	 * Get the registry path of an item.
	 *
	 * @param v 物品 / the item
	 * @return 注册路径 / the registry path
	 */
	public static String getPath(Item v) {
		return getRegistryName(v).getPath();
	}

	/**
	 * 获取流体的注册路径。
	 * <p>
	 * Get the registry path of a fluid.
	 *
	 * @param v 流体 / the fluid
	 * @return 注册路径 / the registry path
	 */
	public static String getPath(Fluid v) {
		return getRegistryName(v).getPath();
	}

	/**
	 * 获取生物群系的注册路径。
	 * <p>
	 * Get the registry path of a biome.
	 *
	 * @param b 生物群系 / the biome
	 * @return 注册路径 / the registry path
	 */
	public static String getPath(Biome b) {
		return getRegistryName(b).getPath();
	}

	/**
	 * 获取物品的注册名。
	 * <p>
	 * Get the registry name of an item.
	 *
	 * @param v 物品 / the item
	 * @return 注册名资源位置 / the registry name resource location
	 */
	public static ResourceLocation getRegistryName(Item v) {
		return ForgeRegistries.ITEMS.getKey(v);
	}
	/** 获取方块的注册名。 / Get the registry name of a block. */
	public static ResourceLocation getRegistryName(Block v) {
		return ForgeRegistries.BLOCKS.getKey(v);
	}
	/** 获取流体的注册名。 / Get the registry name of a fluid. */
	public static ResourceLocation getRegistryName(Fluid v) {
		return ForgeRegistries.FLUIDS.getKey(v);
	}
	/**
	 * 获取生物群系的注册名。注意：运行时不可用此方法获取ResourceKey。
	 * <p>
	 * Get the registry name of a biome. Caution: ResourceKey is not available using this method at runtime.
	 *
	 * @param b 生物群系 / the biome
	 * @return 注册名资源位置 / the registry name resource location
	 */
	public static ResourceLocation getRegistryName(Biome b) {
		return ForgeRegistries.BIOMES.getKey(b);
	}
	/**
	 * 在运行时获取生物群系的注册名（通过RegistryAccess）。
	 * <p>
	 * Get the biome registry name at runtime (via RegistryAccess).
	 *
	 * @param level 世界读取器 / the level reader
	 * @param biome 生物群系 / the biome
	 * @return 注册名资源位置 / the registry name resource location
	 */
	public static ResourceLocation getBiomeKeyRuntime(LevelReader level, Biome biome) {
		return level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
	}
	/**
	 * 根据资源位置获取物品。
	 * <p>
	 * Get an item by resource location.
	 *
	 * @param rl 资源位置 / the resource location
	 * @return 物品 / the item
	 */
	public static Item getItem(ResourceLocation rl) {
		return ForgeRegistries.ITEMS.getValue(rl);
	}
	/**
	 * 根据资源位置获取物品，不存在时抛出异常。
	 * <p>
	 * Get an item by resource location, throwing an exception if it does not exist.
	 *
	 * @param rl 资源位置 / the resource location
	 * @return 物品 / the item
	 * @throws IllegalStateException 如果物品不存在 / if the item does not exist
	 */
	public static Item getItemThrow(ResourceLocation rl) {
		if(!ForgeRegistries.ITEMS.containsKey(rl)){
			throw new IllegalStateException("Item: " + rl + " does not exist");
		}
		return ForgeRegistries.ITEMS.getValue(rl);
	}
	public static Item getColoredItem(DyeColor color, String pathSuffix) {
		return getColoredItem(color, "minecraft", pathSuffix);
	}
	public static Item getColoredItem(DyeColor color, String namespace, String pathSuffix) {
		var loc = new ResourceLocation(namespace + ":" + color.getName() + "_" + pathSuffix);
		var item = ForgeRegistries.ITEMS.getValue(loc);
		if (item == null) {
			var block = ForgeRegistries.BLOCKS.getValue(loc);
			item = block == null ? Items.AIR : block.asItem();
		}
		return item;
	}
	/** 根据资源位置获取药水效果。 / Get a mob effect by resource location. */
	public static MobEffect getEffect(ResourceLocation rl) {
		return ForgeRegistries.MOB_EFFECTS.getValue(rl);
	}
	/** 获取结构的注册名。 / Get the registry name of a structure. */
	public static ResourceLocation getRegistryName(Structure observatory) {
		return BuiltInRegistries.STRUCTURE_TYPE.getKey(observatory.type());
	}
	/** 获取村民职业的注册名。 / Get the registry name of a villager profession. */
	public static ResourceLocation getRegistryName(VillagerProfession prof) {
		return ForgeRegistries.VILLAGER_PROFESSIONS.getKey(prof);
	}
	/** 获取实体类型的注册名。 / Get the registry name of an entity type. */
	public static ResourceLocation getRegistryName(EntityType<?> e) {
		return ForgeRegistries.ENTITY_TYPES.getKey(e);
	}
	/** 获取附魔的注册名。 / Get the registry name of an enchantment. */
	public static ResourceLocation getRegistryName(Enchantment enchID) {
		return ForgeRegistries.ENCHANTMENTS.getKey(enchID);
	}
	/** 根据资源位置获取方块。 / Get a block by resource location. */
	public static Block getBlock(ResourceLocation resourceLocation) {
		return ForgeRegistries.BLOCKS.getValue(resourceLocation);
	}
	/**
	 * 根据资源位置获取方块，不存在时抛出异常。
	 * <p>
	 * Get a block by resource location, throwing an exception if it does not exist.
	 *
	 * @param resourceLocation 资源位置 / the resource location
	 * @return 方块 / the block
	 * @throws IllegalStateException 如果方块不存在 / if the block does not exist
	 */
	public static Block getBlockThrow(ResourceLocation resourceLocation) {
		if(!ForgeRegistries.BLOCKS.containsKey(resourceLocation)){
			throw new IllegalStateException("Block: " + resourceLocation + " does not exist");
		}
		return ForgeRegistries.BLOCKS.getValue(resourceLocation);
	}
	/** 根据资源位置获取村民职业。 / Get a villager profession by resource location. */
	public static VillagerProfession getProfess(ResourceLocation resourceLocation) {
		return ForgeRegistries.VILLAGER_PROFESSIONS.getValue(resourceLocation);
	}
	/** 根据资源位置获取流体。 / Get a fluid by resource location. */
	public static Fluid getFluid(ResourceLocation resourceLocation) {
		return ForgeRegistries.FLUIDS.getValue(resourceLocation);
	}
	/** 获取所有已注册的方块。 / Get all registered blocks. */
	public static Collection<Block> getBlocks() {
		return ForgeRegistries.BLOCKS.getValues();
	}
	/** 获取所有已注册的物品。 / Get all registered items. */
	public static Collection<Item> getItems() {
		return ForgeRegistries.ITEMS.getValues();
	}
	/** 获取所有已注册物品的Holder流。 / Get a stream of Holders for all registered items. */
	public static Stream<Holder<Item>> getItemHolders() {
		return ForgeRegistries.ITEMS.getValues().stream().flatMap(t->ForgeRegistries.ITEMS.getHolder(t).stream());
	}
	/** 根据资源位置获取附魔。 / Get an enchantment by resource location. */
	public static Enchantment getEnchantment(ResourceLocation enchID) {
		return ForgeRegistries.ENCHANTMENTS.getValue(enchID);
	}
	/** 根据资源位置获取实体类型。 / Get an entity type by resource location. */
	public static EntityType<?> getEntity(ResourceLocation resourceLocation) {
		return ForgeRegistries.ENTITY_TYPES.getValue(resourceLocation);
	}
	/** 获取所有已注册的实体类型。 / Get all registered entity types. */
	public static Collection<EntityType<?>> getEntities() {
		return ForgeRegistries.ENTITY_TYPES.getValues();
	}
	/** 根据标签键获取方块标签。 / Get a block tag by tag key. */
	public static ITag<Block> getBlockTag(TagKey<Block> tag){
		return ForgeRegistries.BLOCKS.tags().getTag(tag);
	}
	/** 根据标签键获取流体标签。 / Get a fluid tag by tag key. */
	public static ITag<Fluid> getFluidTag(TagKey<Fluid> tag){
		return ForgeRegistries.FLUIDS.tags().getTag(tag);
	}
	/** 根据标签键获取物品标签。 / Get an item tag by tag key. */
	public static ITag<Item> getItemTag(TagKey<Item> tag){
		return ForgeRegistries.ITEMS.tags().getTag(tag);
	}
}
