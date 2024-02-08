package com.teammoeg.frostedheart.util;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
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
		return ForgeRegistries.BIOMES.getKey(b);
	}
	public static ResourceLocation getRegistryName(Structure<NoFeatureConfig> observatory) {
		return ForgeRegistries.STRUCTURE_FEATURES.getKey(observatory);
	}
	public static ResourceLocation getRegistryName(VillagerProfession prof) {
		return ForgeRegistries.PROFESSIONS.getKey(prof);
	}
	public static ResourceLocation getRegistryName(EntityType<?> e) {
		return ForgeRegistries.ENTITIES.getKey(e);
	}
}
