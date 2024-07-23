package com.teammoeg.frostedheart.util;

import java.util.Collection;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.registries.ForgeRegistries;

public class RegistryUtils {
	public static ResourceLocation getRegistryName(Item v) {
		return ForgeRegistries.ITEMS.getKey(v);
	}
	public static ResourceLocation getRegistryName(Block v) {
		return ForgeRegistries.BLOCKS.getKey(v);
	}
	public static ResourceLocation getRegistryName(Fluid v) {
		return ForgeRegistries.FLUIDS.getKey(v);
	}
	public static ResourceLocation getRegistryName(Biome b) {
		return b.getRegistryName();
	}
	public static Item getItem(ResourceLocation rl) {
		return ForgeRegistries.ITEMS.getValue(rl);
	}
	public static Item getItemThrow(ResourceLocation rl) {
		if(!ForgeRegistries.ITEMS.containsKey(rl))
			new IllegalStateException("Item: " + rl + " does not exist");
		return ForgeRegistries.ITEMS.getValue(rl);
	}
	public static MobEffect getEffect(ResourceLocation rl) {
		return ForgeRegistries.POTIONS.getValue(rl);
	}
	public static ResourceLocation getRegistryName(StructureFeature<NoneFeatureConfiguration> observatory) {
		return observatory.getRegistryName();
	}
	public static ResourceLocation getRegistryName(VillagerProfession prof) {
		return prof.getRegistryName();
	}
	public static ResourceLocation getRegistryName(EntityType<?> e) {
		return e.getRegistryName();
	}
	public static ResourceLocation getRegistryName(Enchantment enchID) {
		return ForgeRegistries.ENCHANTMENTS.getKey(enchID);
	}
	public static Block getBlock(ResourceLocation resourceLocation) {
		return ForgeRegistries.BLOCKS.getValue(resourceLocation);
	}
	public static VillagerProfession getProfess(ResourceLocation resourceLocation) {
		return ForgeRegistries.PROFESSIONS.getValue(resourceLocation);
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
	public static Enchantment getEnchantment(ResourceLocation enchID) {
		return ForgeRegistries.ENCHANTMENTS.getValue(enchID);
	}
	public static EntityType<?> getEntity(ResourceLocation resourceLocation) {
		return ForgeRegistries.ENTITIES.getValue(resourceLocation);
	}
	public static Collection<EntityType<?>> getEntities() {
		return ForgeRegistries.ENTITIES.getValues();
	}
}
