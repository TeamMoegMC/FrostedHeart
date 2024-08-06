package com.teammoeg.frostedheart.loot;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistries.Keys;

public class FHLoot {
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTRY = DeferredRegister.create(Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, FHMain.MODID);
	static {
		REGISTRY.register("remove_loot", () -> RemoveLootModifier.CODEC);
		REGISTRY.register("replace_loot", () -> ReplaceLootModifier.CODEC);
		REGISTRY.register("dechant", () -> DechantLootModifier.CODEC);
		REGISTRY.register("damage",()->ApplyDamageLootModifier.CODEC);
		REGISTRY.register("add_loot",()-> AddLootModifier.CODEC);
		TemperatureLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "temperature"),
			new LootItemConditionType(new TemperatureLootCondition.Serializer()));
		TagLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "block_tag"), new LootItemConditionType(new TagLootCondition.Serializer()));
		TreasureLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "treasure"), new LootItemConditionType(new TreasureLootCondition.Serializer()));
		ModLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "modids"), new LootItemConditionType(new ModLootCondition.Serializer()));
		BlizzardDamageCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "blizzard_damage"),
			new LootItemConditionType(new BlizzardDamageCondition.Serializer()));
	}

	public FHLoot() {
		// TODO Auto-generated constructor stub
	}

}
