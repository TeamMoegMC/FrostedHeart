/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.listeners;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.model.DynamicBlockModelReference;
import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ChordaClientRegistry {

	public ChordaClientRegistry() {
		// TODO Auto-generated constructor stub
	}
	@SubscribeEvent
	public static void registerModels(ModelEvent.RegisterAdditional ev)
	{
		Chorda.LOGGER.info("===========Dynamic Model Register========");
		DynamicBlockModelReference.registeredModels.forEach(rl->{
			ev.register(rl);
			Chorda.LOGGER.info(rl);
		});
	}
	@SubscribeEvent
	public static void onCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
		CreativeTabItemHelper helper = new CreativeTabItemHelper(event.getTabKey());
		ForgeRegistries.ITEMS.forEach(e -> {
			if (e instanceof ICreativeModeTabItem item) {
				item.fillItemCategory(helper);
			}
		});
		helper.register(event);
	
	}
}
