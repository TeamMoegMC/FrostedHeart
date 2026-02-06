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
 * Events fired only on logical server side.
 *
 * It really can be part of CommonEvents, but this is just for organization.
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChordaServerEvents {
    @SubscribeEvent
    public static void serverLevelSave(final ServerLevelDataSaveEvent event) {
        if (CTeamDataManager.INSTANCE != null) {
            CTeamDataManager.INSTANCE.save();
        }
    }

    // Server Lifecycle Events

    @SubscribeEvent
    public static void serverAboutToStart(final ServerAboutToStartEvent event) {
        new CTeamDataManager();
        
    }

    @SubscribeEvent
    public static void serverStarting(final ServerStartingEvent event) {
    	CTeamDataManager.INSTANCE.load();
    }

    @SubscribeEvent
    public static void serverStarted(final ServerStartedEvent event) {

    }

    @SubscribeEvent
    public static void serverStopping(final ServerStoppingEvent event) {

    }

    @SubscribeEvent
    public static void serverStopped(final ServerStoppedEvent event) {
        CTeamDataManager.INSTANCE = null;
    }
}
