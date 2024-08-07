package com.teammoeg.frostedheart.loot;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries.Keys;

public class FHLoot {
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LM_REGISTRY = DeferredRegister.create(Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, FHMain.MODID);
	public static final DeferredRegister<LootItemConditionType> LC_REGISTRY = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, FHMain.MODID);
	static {
		LM_REGISTRY.register("remove_loot", () -> RemoveLootModifier.CODEC);
		LM_REGISTRY.register("replace_loot", () -> ReplaceLootModifier.CODEC);
		LM_REGISTRY.register("dechant", () -> DechantLootModifier.CODEC);
		LM_REGISTRY.register("damage",()->ApplyDamageLootModifier.CODEC);
		LM_REGISTRY.register("add_loot",()-> AddLootModifier.CODEC);
		TemperatureLootCondition.TYPE = LC_REGISTRY.register("temperature",()->new LootItemConditionType(new TemperatureLootCondition.Serializer()));
		TagLootCondition.TYPE = LC_REGISTRY.register("block_tag",()-> new LootItemConditionType(new TagLootCondition.Serializer()));
		TreasureLootCondition.TYPE = LC_REGISTRY.register("treasure",()-> new LootItemConditionType(new TreasureLootCondition.Serializer()));
		ModLootCondition.TYPE = LC_REGISTRY.register("modids",()-> new LootItemConditionType(new ModLootCondition.Serializer()));
		BlizzardDamageCondition.TYPE = LC_REGISTRY.register("blizzard_damage",()->new LootItemConditionType(new BlizzardDamageCondition.Serializer()));
	}

	public FHLoot() {
		// TODO Auto-generated constructor stub
	}

}
