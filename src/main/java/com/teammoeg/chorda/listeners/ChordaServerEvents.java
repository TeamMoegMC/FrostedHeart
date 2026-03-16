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
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.events.ServerLevelDataSaveEvent;

import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 仅在逻辑服务端触发的事件监听器，管理服务器生命周期和队伍数据的加载/保存。
 * 本质上可以是CommonEvents的一部分，但为了组织清晰而单独分离。
 * <p>
 * Event listener fired only on the logical server side, managing server lifecycle
 * and team data loading/saving. Could be part of CommonEvents, but separated for organization.
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChordaServerEvents {
    /**
     * 处理服务端世界数据保存事件，保存队伍数据。
     * <p>
     * Handles server level data save events, saving team data.
     *
     * @param event 世界数据保存事件 / The server level data save event
     */
    @SubscribeEvent
    public static void serverLevelSave(final ServerLevelDataSaveEvent event) {
        if (CTeamDataManager.INSTANCE != null) {
            CTeamDataManager.INSTANCE.save();
        }
    }

    // Server Lifecycle Events

    /**
     * 处理服务器即将启动事件，创建队伍数据管理器实例。
     * <p>
     * Handles server about to start event, creating the team data manager instance.
     *
     * @param event 服务器即将启动事件 / The server about to start event
     */
    @SubscribeEvent
    public static void serverAboutToStart(final ServerAboutToStartEvent event) {
        new CTeamDataManager();
        
    }

    /**
     * 处理服务器启动中事件，加载队伍数据。
     * <p>
     * Handles server starting event, loading team data.
     *
     * @param event 服务器启动中事件 / The server starting event
     */
    @SubscribeEvent
    public static void serverStarting(final ServerStartingEvent event) {
    	CTeamDataManager.INSTANCE.load();
    }

    /**
     * 处理服务器启动完成事件。
     * <p>
     * Handles server started event.
     *
     * @param event 服务器启动完成事件 / The server started event
     */
    @SubscribeEvent
    public static void serverStarted(final ServerStartedEvent event) {

    }

    /**
     * 处理服务器正在停止事件。
     * <p>
     * Handles server stopping event.
     *
     * @param event 服务器正在停止事件 / The server stopping event
     */
    @SubscribeEvent
    public static void serverStopping(final ServerStoppingEvent event) {

    }

    /**
     * 处理服务器已停止事件，清除队伍数据管理器实例。
     * <p>
     * Handles server stopped event, clearing the team data manager instance.
     *
     * @param event 服务器已停止事件 / The server stopped event
     */
    @SubscribeEvent
    public static void serverStopped(final ServerStoppedEvent event) {
        CTeamDataManager.INSTANCE = null;
    }
}
