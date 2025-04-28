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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.events.ServerLevelDataSaveEvent;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.clusterserver.ServerConnectionHelper;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.content.climate.player.TemperatureUpdate;
import com.teammoeg.frostedheart.restarter.TssapProtocolHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Events fired only on logical server side.
 *
 * It really can be part of FHCommonEvents, but this is just for organization.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FHServerEvents {
	@SubscribeEvent
	public static void serverLevelSave(final ServerLevelDataSaveEvent event) {
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
		if(FMLEnvironment.dist==Dist.DEDICATED_SERVER) {
			TssapProtocolHandler.serverPrepareUpdateReminder();
			File authConfig=new File(FMLPaths.CONFIGDIR.get().toFile(),"auth.json");
			if(authConfig.exists()) {
				try {
					JsonObject authCfg=JsonParser.parseString(FileUtil.readString(authConfig)).getAsJsonObject();
					SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
					PBEKeySpec spec = new PBEKeySpec(authCfg.get("key").getAsString().toCharArray(), "Frostedheart".getBytes(StandardCharsets.UTF_8), 65536, 256);
					SecretKey tmp = factory.generateSecret(spec);
					ServerConnectionHelper.currentKey = new SecretKeySpec(tmp.getEncoded(), "AES");
					ServerConnectionHelper.isAuthEnabled=authCfg.get("enabled").getAsBoolean();
					ServerConnectionHelper.timeout=authCfg.get("timeout").getAsLong();
					ServerConnectionHelper.loginServer=authCfg.get("loginServer").getAsString();
				} catch (JsonSyntaxException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
					e.printStackTrace();
				}
				
			}
		}
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
