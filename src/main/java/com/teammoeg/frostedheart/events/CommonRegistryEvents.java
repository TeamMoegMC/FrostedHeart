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

import javax.annotation.Nonnull;

import com.teammoeg.frostedheart.FHAttributes;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.loot.AddLootModifier;
import com.teammoeg.frostedheart.loot.ApplyDamageLootModifier;
import com.teammoeg.frostedheart.loot.BlizzardDamageCondition;
import com.teammoeg.frostedheart.loot.DechantLootModifier;
import com.teammoeg.frostedheart.loot.ModLootCondition;
import com.teammoeg.frostedheart.loot.RemoveLootModifier;
import com.teammoeg.frostedheart.loot.ReplaceLootModifier;
import com.teammoeg.frostedheart.loot.TagLootCondition;
import com.teammoeg.frostedheart.loot.TemperatureLootCondition;
import com.teammoeg.frostedheart.loot.TreasureLootCondition;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.frostedheart.world.FHStructureFeatures;
import com.teammoeg.frostedheart.world.FHStructures;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonRegistryEvents {

   /* @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void onDimensionRegistry(RegistryEvent.Register event) {
        //FHDimensions.register();

    }*/
/*
    @SubscribeEvent
    public static void onFeatureRegistry(RegistryEvent.Register<Feature<?>> event) {
        event.getRegistry().registerAll(FHFeatures.FHORE.setRegistryName(FHMain.MODID, "fhore"));
        event.getRegistry().register(FHFeatures.SPACECRAFT.setRegistryName(FHMain.MODID, "spacecraft"));
        event.getRegistry().register(FHFeatures.FLOWER_COVERED_DEPOSIT_FEATURE.setRegistryName("flower_covered_deposit"));
    }

    @SubscribeEvent
    public static void onStructureRegistry(RegistryEvent.Register<StructureFeature<?>> event) {
        event.getRegistry().registerAll(FHStructures.OBSERVATORY.setRegistryName(FHMain.MODID, "observatory"));

        FHStructureFeatures.registerStructureFeatures();
    }
*/
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
}
