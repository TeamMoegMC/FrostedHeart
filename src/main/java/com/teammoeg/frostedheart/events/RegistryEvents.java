/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.events;

import com.cannolicatfish.rankine.init.RankineBlocks;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import com.teammoeg.frostedheart.content.decoration.FHOreBlock;
import com.teammoeg.frostedheart.loot.*;
import com.teammoeg.frostedheart.util.FHLogger;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.frostedheart.world.FHStructureFeatures;
import com.teammoeg.frostedheart.world.FHStructures;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.loot.LootConditionType;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;

import static com.teammoeg.frostedheart.FHContent.*;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryEvents {
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        FHBlocks.fluorite_ore = new FHOreBlock("fluorite_ore", RankineBlocks.DEF_ORE.harvestLevel(3), FHBlockItem::new);

        for (Block block : registeredFHBlocks) {
            try {
                event.getRegistry().register(block);
            } catch (Throwable e) {
                FHLogger.error("Failed to register a block. ({})", block);
                throw e;
            }
        }
    }

    @SubscribeEvent
    public static void registerModifierSerializers(@Nonnull final RegistryEvent.Register<GlobalLootModifierSerializer<?>> event) {
        IForgeRegistry<GlobalLootModifierSerializer<?>> registry = event.getRegistry();
        registry.register(new RemoveLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "remove_loot")));
        registry.register(new ReplaceLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "replace_loot")));
        registry.register(new DechantLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "dechant")));
        registry.register(new ApplyDamageLootModifier.Serializer().setRegistryName(new ResourceLocation(FHMain.MODID, "damage")));
        TemperatureLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "temperature"), new LootConditionType(new TemperatureLootCondition.Serializer()));
        TagLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "block_tag"), new LootConditionType(new TagLootCondition.Serializer()));
        TreasureLootCondition.TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(FHMain.MODID, "treasure"), new LootConditionType(new TreasureLootCondition.Serializer()));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : registeredFHItems) {
            try {
                event.getRegistry().register(item);
            } catch (Throwable e) {
                FHLogger.error("Failed to register an item. ({}, {})", item, item.getRegistryName());
                throw e;
            }
        }
    }

    @SubscribeEvent
    public static void registerFluids(RegistryEvent.Register<Fluid> event) {
        for (Fluid fluid : registeredFHFluids) {
            try {
                event.getRegistry().register(fluid);
            } catch (Throwable e) {
                FHLogger.error("Failed to register a fluid. ({}, {})", fluid, fluid.getRegistryName());
                throw e;
            }
        }
    }

    @SubscribeEvent
    public static void registerEffects(final RegistryEvent.Register<Effect> event) {
        FHEffects.registerAll(event.getRegistry());
    }

    /**
     * @param event
     */
    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void onDimensionRegistry(RegistryEvent.Register event) {
        //FHDimensions.register();

    }

    @SubscribeEvent
    public static void onFeatureRegistry(RegistryEvent.Register<Feature<?>> event) {
        event.getRegistry().registerAll(FHFeatures.FHORE.setRegistryName(FHMain.MODID, "fhore"),
                FHFeatures.SPACECRAFT.setRegistryName(FHMain.MODID, "spacecraft"));

    }

    @SubscribeEvent
    public static void onStructureRegistry(RegistryEvent.Register<Structure<?>> event) {
        event.getRegistry().registerAll(FHStructures.OBSERVATORY.setRegistryName(FHMain.MODID, "observatory"));

        FHStructureFeatures.registerStructureFeatures();
    }
}
