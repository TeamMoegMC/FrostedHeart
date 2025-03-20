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

package com.teammoeg.frostedheart.events;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.content.climate.player.TemperatureUpdate;

import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Events fired only on logical server side.
 *
 * It really can be part of FHCommonEvents, but this is just for organization.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FHServerEvents {
	@SubscribeEvent
	public static void serverLevelSave(final LevelEvent.Save event) {
	}

	@SubscribeEvent
	public static void serverTick(final ServerTickEvent event) {
		if (event.phase == Phase.START) {
			if(TemperatureUpdate.threadingPool!=null)
				TemperatureUpdate.threadingPool.tick();
		}
	}

	// Server Lifecycle Events
	/**
	 * Server about to start, config and datapack is loaded during this phase, fires
	 * before level loaded
	 */
	@SubscribeEvent
	public static void serverAboutToStart(final ServerAboutToStartEvent event) {
		SurroundingTemperatureSimulator.init();
		TemperatureUpdate.init();
	}

	/**
	 * Server starting, all things are loaded properly, canceling this event
	 * prevents server from start
	 */
	@SubscribeEvent
	public static void serverStarting(final ServerStartingEvent event) {

	}

	/**
	 * Server started. fires immediately after server starting.
	 */
	@SubscribeEvent
	public static void serverStarted(final ServerStartedEvent event) {

	}

	/**
	 * Server stopping, server thread is stopped and server tick is stopped, but
	 * nothing is done to stop the server Suitable for data saving
	 */
	@SubscribeEvent
	public static void serverStopping(final ServerStoppingEvent event) {

	}

	/**
	 * Server stopped, all data saved, suitable for resource cleanup.
	 */
	@SubscribeEvent
	public static void serverStopped(final ServerStoppedEvent event) {
		CTeamDataManager.INSTANCE = null;
		TemperatureUpdate.shutdown();
	}
}
