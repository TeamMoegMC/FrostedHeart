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

package com.teammoeg.frostedscenario;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;

public class FSConfig {
	/**
	 * Config value that only in client
	 * 
	 */
	public static class Client {

		public final ForgeConfigSpec.BooleanValue autoMode;
		public final ForgeConfigSpec.IntValue autoModeInterval;
		public final ForgeConfigSpec.DoubleValue textSpeed;
		public final ForgeConfigSpec.BooleanValue renderScenario;
		public final ForgeConfigSpec.IntValue scenarioRenderQuality;
		public final ForgeConfigSpec.IntValue scenarioRenderThread;
		public final ForgeConfigSpec.BooleanValue scenarioAntiAliasing;

		Client(ForgeConfigSpec.Builder builder) {
			builder.push("Scenario");
			renderScenario = builder.comment("Enables the scenario act hud rendering. ")
				.define("renderScenario", true); // todo: set true
			autoMode = builder.comment("Enables Auto click when scenario requires")
				.define("autoMode", true);
			autoModeInterval = builder.comment("Tick before click when a click is required to progress")
				.defineInRange("autoModeInterval", 40, 0, 500);
			textSpeed = builder.comment("Base text appear speed, actual speed may change by scenario if necessary, speed 1 is 0.5 character per tick.")
				.defineInRange("textSpeed", 1d, 0.000001, 100000);
			scenarioRenderQuality = builder
				.comment("Scenario 2d content rendering quality, internal resolution=2^(config value)*1024, 2d contents are rendered on cpu, higher quality may cause slower rendering")
				.defineInRange("scenarioRenderQuality", 2, 0, 16);
			scenarioRenderThread = builder.comment("Scenario rendering thread, Scenario screen are pre-rendered in seperate pool to prevent lag")
				.defineInRange("scenarioRenderThread", 2, 1, 16);
			scenarioAntiAliasing = builder.comment("Scenario rendering Antialiasing, turn off to higher performance")
				.define("scenarioAntiAliasing", true);
			builder.pop();
		}

		public int getScenarioScale() {
			return 1 << scenarioRenderQuality.get();
		}
	}

	/**
	 * Config value that would NOT sync between client and server
	 * 
	 */
	public static class Common {

		Common(ForgeConfigSpec.Builder builder) {


		}
	}

	/**
	 * Config value that would sync between client and server
	 * 
	 */
	public static class Server {

		public final ForgeConfigSpec.ConfigValue<Boolean> enableScenario;
		Server(ForgeConfigSpec.Builder builder) {
			builder.push("Scenario");
			enableScenario = builder
				.comment("Enables the scenario system. ")
				.define("enableScenario", true);
			builder.pop();
		}
	}

	public enum TempOrbPos {
		MIDDLE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}

	public static final ForgeConfigSpec CLIENT_CONFIG;
	public static final ForgeConfigSpec COMMON_CONFIG;
	public static final ForgeConfigSpec SERVER_CONFIG;
	public static final Client CLIENT;
	public static final Common COMMON;
	public static final Server SERVER;

	public static ArrayList<String> DEFAULT_WHITELIST = new ArrayList<>();

	static {
		ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
		CLIENT = new Client(CLIENT_BUILDER);
		CLIENT_CONFIG = CLIENT_BUILDER.build();
		ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
		COMMON = new Common(COMMON_BUILDER);
		COMMON_CONFIG = COMMON_BUILDER.build();
		ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
		SERVER = new Server(SERVER_BUILDER);
		SERVER_CONFIG = SERVER_BUILDER.build();
	}

	public static void register() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, FSConfig.CLIENT_CONFIG);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FSConfig.SERVER_CONFIG);
	}
}
