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

package com.teammoeg.chorda.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
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

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;

public class CRegistryHelper {
	public static String getPath(Block v) {
		return getRegistryName(v).getPath();
	}

	public static String getPath(Item v) {
		return getRegistryName(v).getPath();
	}

	public static String getPath(Fluid v) {
		return getRegistryName(v).getPath();
	}

	public static String getPath(Biome b) {
		return getRegistryName(b).getPath();
	}

	public static ResourceLocation getRegistryName(Item v) {
		return ForgeRegistries.ITEMS.getKey(v);
	}
	public static ResourceLocation getRegistryName(Block v) {
		return ForgeRegistries.BLOCKS.getKey(v);
	}
	public static ResourceLocation getRegistryName(Fluid v) {
		return ForgeRegistries.FLUIDS.getKey(v);
	}
	// Caution: RK is not available using this method on runtime
	public static ResourceLocation getRegistryName(Biome b) {
		return ForgeRegistries.BIOMES.getKey(b);
	}
	public static ResourceLocation getBiomeKeyRuntime(LevelReader level, Biome biome) {
		return level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
	}
	public static Item getItem(ResourceLocation rl) {
		return ForgeRegistries.ITEMS.getValue(rl);
	}
	public static Item getItemThrow(ResourceLocation rl) {
		if(!ForgeRegistries.ITEMS.containsKey(rl)){
			throw new IllegalStateException("Item: " + rl + " does not exist");
		}
		return ForgeRegistries.ITEMS.getValue(rl);
	}
	public static MobEffect getEffect(ResourceLocation rl) {
		return ForgeRegistries.MOB_EFFECTS.getValue(rl);
	}
	public static ResourceLocation getRegistryName(Structure observatory) {
		return BuiltInRegistries.STRUCTURE_TYPE.getKey(observatory.type());
	}
	public static ResourceLocation getRegistryName(VillagerProfession prof) {
		return ForgeRegistries.VILLAGER_PROFESSIONS.getKey(prof);
	}
	public static ResourceLocation getRegistryName(EntityType<?> e) {
		return ForgeRegistries.ENTITY_TYPES.getKey(e);
	}
	public static ResourceLocation getRegistryName(Enchantment enchID) {
		return ForgeRegistries.ENCHANTMENTS.getKey(enchID);
	}
	public static Block getBlock(ResourceLocation resourceLocation) {
		return ForgeRegistries.BLOCKS.getValue(resourceLocation);
	}
	public static Block getBlockThrow(ResourceLocation resourceLocation) {
		if(!ForgeRegistries.BLOCKS.containsKey(resourceLocation)){
			throw new IllegalStateException("Block: " + resourceLocation + " does not exist");
		}
		return ForgeRegistries.BLOCKS.getValue(resourceLocation);
	}
	public static VillagerProfession getProfess(ResourceLocation resourceLocation) {
		return ForgeRegistries.VILLAGER_PROFESSIONS.getValue(resourceLocation);
	}
	public static Fluid getFluid(ResourceLocation resourceLocation) {
		return ForgeRegistries.FLUIDS.getValue(resourceLocation);
	}
	public static Collection<Block> getBlocks() {
		return ForgeRegistries.BLOCKS.getValues();
	}
	public static Collection<Item> getItems() {
		return ForgeRegistries.ITEMS.getValues();
	}
	public static Stream<Holder<Item>> getItemHolders() {
		return ForgeRegistries.ITEMS.getValues().stream().flatMap(t->ForgeRegistries.ITEMS.getHolder(t).stream());
	}
	public static Enchantment getEnchantment(ResourceLocation enchID) {
		return ForgeRegistries.ENCHANTMENTS.getValue(enchID);
	}
	public static EntityType<?> getEntity(ResourceLocation resourceLocation) {
		return ForgeRegistries.ENTITY_TYPES.getValue(resourceLocation);
	}
	public static Collection<EntityType<?>> getEntities() {
		return ForgeRegistries.ENTITY_TYPES.getValues();
	}
	public static ITag<Block> getBlockTag(TagKey<Block> tag){
		return ForgeRegistries.BLOCKS.tags().getTag(tag);
	}
	public static ITag<Fluid> getFluidTag(TagKey<Fluid> tag){
		return ForgeRegistries.FLUIDS.tags().getTag(tag);
	}
	public static ITag<Item> getItemTag(TagKey<Item> tag){
		return ForgeRegistries.ITEMS.tags().getTag(tag);
	}
}
