package com.teammoeg.frostedresearch.handler;

import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRMain;

import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FHServerEvents {
	@SubscribeEvent
	public static void serverLevelSave(final LevelEvent.Save event) {
		FHResearch.save();
	}

	@SubscribeEvent
	public static void serverTick(final ServerTickEvent event) {
	}

	// Server Lifecycle Events
	/**
	 * Server about to start, config and datapack is loaded during this phase, fires
	 * before level loaded
	 */
	@SubscribeEvent
	public static void serverAboutToStart(final ServerAboutToStartEvent event) {
		FHResearch.load();
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

	}
}
