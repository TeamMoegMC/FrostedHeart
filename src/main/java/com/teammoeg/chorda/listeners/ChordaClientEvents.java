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
/**
 * Chorda客户端Forge事件监听器，处理渲染Tick和客户端Tick事件。
 * 负责推进部分Tick计时器和客户端数据存储的检查与保存。
 * <p>
 * Chorda client-side Forge event listener handling render tick and client tick events.
 * Responsible for advancing the partial tick timer and checking/saving client data storage.
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChordaClientEvents {
	/**
	 * 处理客户端渲染Tick事件，在未暂停时推进部分Tick计时器。
	 * <p>
	 * Handles client render tick events, advancing the partial tick timer when not paused.
	 *
	 * @param ev 渲染Tick事件 / The render tick event
	 */
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public static void onClientRenderTick(TickEvent.RenderTickEvent ev) {
		if(ev.phase==Phase.START) {
			if(!Minecraft.getInstance().isPaused())
				PartialTickTracker.getInstance().advanceTimer();
		}
	}

	/**
	 * 处理客户端Tick事件。开始阶段推进Tick计时器，结束阶段检查并保存客户端数据。
	 * <p>
	 * Handles client tick events. Advances tick timer at start phase, checks and saves client data at end phase.
	 *
	 * @param ev 客户端Tick事件 / The client tick event
	 */
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
