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
import com.teammoeg.chorda.recipe.RecipeReloadListener;
import com.teammoeg.chorda.recipe.ToolActionIngredient;
import com.teammoeg.chorda.scheduler.SchedulerQueue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * Chorda通用Forge事件监听器，处理服务端Tick和数据包重载事件。
 * <p>
 * Chorda common Forge event listener handling server tick and datapack reload events.
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChordaCommonEvents {
    /**
     * 处理服务端世界Tick事件，执行调度队列中的定时任务。
     * <p>
     * Handles server level tick events, executing scheduled tasks from the scheduler queue.
     *
     * @param event 世界Tick事件 / The level tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            Level world = event.level;
            if (!world.isClientSide && world instanceof ServerLevel) {
                ServerLevel serverWorld = (ServerLevel) world;

                // Scheduled checks (e.g. town structures)
                SchedulerQueue.tickAll(serverWorld);
            }
        }
    }
    /**
     * 添加数据包重载监听器，用于在数据包重载时重新处理配方数据。
     * <p>
     * Adds datapack reload listeners for reprocessing recipe data on datapack reload.
     *
     * @param event 添加重载监听器事件 / The add reload listener event
     */
    @SubscribeEvent
    public static void addReloadListenersLowest(AddReloadListenerEvent event) {
        ReloadableServerResources dataPackRegistries = event.getServerResources();
        event.addListener(new RecipeReloadListener(dataPackRegistries));
       
    }
  
}
