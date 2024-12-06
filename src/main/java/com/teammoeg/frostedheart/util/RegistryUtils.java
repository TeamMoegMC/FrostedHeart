package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.stream.Stream;

public class RegistryUtils {
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
	public static RegistryAccess getAccess() {
		MinecraftServer server=FHTeamDataManager.getServer();
		if(server!=null)
			return server.registryAccess();
		return Minecraft.getInstance().level.registryAccess();
	}
	public static ResourceLocation getRegistryName(Biome b) {
		return ForgeRegistries.BIOMES.getKey(b);
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
}
