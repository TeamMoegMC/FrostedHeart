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

package com.teammoeg.frostedheart;

import static com.teammoeg.frostedheart.content.FHContent.customModels;
import static com.teammoeg.frostedheart.content.FHContent.registeredFHBlocks;
import static com.teammoeg.frostedheart.content.FHContent.registeredFHFluids;
import static com.teammoeg.frostedheart.content.FHContent.registeredFHItems;

import java.util.Map;

import com.stereowalker.survive.potion.SEffects;
import com.teammoeg.frostedheart.content.FHEffects;
import com.teammoeg.frostedheart.util.FHLogger;
import com.teammoeg.frostedheart.world.FHFeatures;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FHRegistryEvents {
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
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

    @SubscribeEvent
	public static void onModelBake(ModelBakeEvent event) {
		Map<ResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();
		
		customModels.entrySet().forEach((ent) ->modelRegistry.put(ent.getKey().delegate.name(),ent.getValue().apply(modelRegistry.get(ent.getKey().delegate.name()))));
	}
    @SubscribeEvent
    public static void onFeatureRegistry(RegistryEvent.Register<Feature<?>> event) {
        event.getRegistry().register(FHFeatures.FHORE.setRegistryName(FHMain.MODID, "fhore"));
    }
}
