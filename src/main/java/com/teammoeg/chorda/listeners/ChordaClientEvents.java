/*
 * Copyright (c) 2024 TeamMoeg
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
import com.teammoeg.chorda.client.PartialTickTracker;
import com.teammoeg.chorda.client.model.DynamicBlockModelReference;
import com.teammoeg.chorda.dataholders.client.CClientDataStorage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChordaClientEvents {
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public static void onClientRenderTick(TickEvent.RenderTickEvent ev) {
		if(ev.phase==Phase.START) {
			if(!Minecraft.getInstance().isPaused())
				PartialTickTracker.getInstance().advanceTimer();
		}
	}

	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public static void onClientTick(TickEvent.ClientTickEvent ev) {
		if(ev.phase==Phase.START) {
			if(!Minecraft.getInstance().isPaused())
				PartialTickTracker.getInstance().tick();
			
		}else {
			CClientDataStorage.checkAndSave();
		}
	}

	
	

}
