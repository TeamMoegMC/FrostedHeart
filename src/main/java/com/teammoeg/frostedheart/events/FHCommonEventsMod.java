/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.events;

import com.teammoeg.frostedheart.FHAttributes;
import com.teammoeg.frostedheart.FHEntityTypes;
import com.teammoeg.frostedheart.FHMain;

import com.teammoeg.frostedheart.foundation.world.entities.CuriosityEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FHCommonEventsMod {

    @SubscribeEvent
    public static void onEntityAttributeModificationEvent(EntityAttributeModificationEvent event) {
		event.add(EntityType.PLAYER, FHAttributes.ENV_TEMPERATURE.get());
		event.add(EntityType.PLAYER, FHAttributes.EFFECTIVE_TEMPERATURE.get());
		event.add(EntityType.PLAYER, FHAttributes.INSULATION.get());
		event.add(EntityType.PLAYER, FHAttributes.WIND_PROOF.get());
		event.add(EntityType.PLAYER, FHAttributes.HEAT_PROOF.get());
	}/*
    @SubscribeEvent
    public static void registerModifierSerializers(@Nonnull final RegistryEvent.Register<GlobalLootModifierSerializer<?>> event) {
        IForgeRegistry<GlobalLootModifierSerializer<?>> registry = event.getRegistry();
        registry.register(new RemoveLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "remove_loot")));
        registry.register(new ReplaceLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "replace_loot")));
        registry.register(new DechantLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "dechant")));
        registry.register(new ApplyDamageLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "damage")));
        registry.register(new AddLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "add_loot")));
        TemperatureLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "temperature"), new LootItemConditionType(new TemperatureLootCondition.Serializer()));
        TagLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "block_tag"), new LootItemConditionType(new TagLootCondition.Serializer()));
        TreasureLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "treasure"), new LootItemConditionType(new TreasureLootCondition.Serializer()));
        ModLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "modids"), new LootItemConditionType(new ModLootCondition.Serializer()));
        BlizzardDamageCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "blizzard_damage"), new LootItemConditionType(new BlizzardDamageCondition.Serializer()));
    }*/

	@SubscribeEvent
	public static void entityAttributes(EntityAttributeCreationEvent event) {
		event.put(FHEntityTypes.CURIOSITY.get(), CuriosityEntity.createAttributes().build());
		event.put(FHEntityTypes.WANDERING_REFUGEE.get(), PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.ARMOR, 2.0F)
				.add(Attributes.MOVEMENT_SPEED, 0.2F)
				.add(Attributes.ATTACK_DAMAGE, 2.0D)
				.build());
	}

	@SubscribeEvent
	public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
		event.register(
				FHEntityTypes.CURIOSITY.get(),
				SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.WORLD_SURFACE,
				CuriosityEntity::canSpawn,
				SpawnPlacementRegisterEvent.Operation.OR
		);

		event.register(
				FHEntityTypes.WANDERING_REFUGEE.get(),
				SpawnPlacements.Type.NO_RESTRICTIONS,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Mob::checkMobSpawnRules,
				SpawnPlacementRegisterEvent.Operation.OR
		);
	}
}
