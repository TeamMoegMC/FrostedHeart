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

package com.teammoeg.frostedheart.infrastructure.config;

import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FHConfig {
	/**
	 * Config value that only in client
	 * 
	 */
	public static class Client {
		public final ForgeConfigSpec.BooleanValue enablesTemperatureOrb;
		public final ForgeConfigSpec.BooleanValue enableUI;
		public final ForgeConfigSpec.IntValue tempOrbOffsetX;
		public final ForgeConfigSpec.IntValue tempOrbOffsetY;
		public final ForgeConfigSpec.EnumValue<TempOrbPos> tempOrbPosition;
		public final ForgeConfigSpec.BooleanValue useFahrenheit;
		public final ForgeConfigSpec.BooleanValue enableFrozenOverlay;
		public final ForgeConfigSpec.BooleanValue enableFrozenVignette;
		public final ForgeConfigSpec.BooleanValue enableHeatVignette;
		public final ForgeConfigSpec.BooleanValue enableFrozenSound;
		public final ForgeConfigSpec.BooleanValue enableBreathParticle;
		public final ForgeConfigSpec.BooleanValue enableWaypoint;
		public final ForgeConfigSpec.BooleanValue autoMode;
		public final ForgeConfigSpec.IntValue autoModeInterval;
		public final ForgeConfigSpec.DoubleValue textSpeed;
		public final ForgeConfigSpec.BooleanValue renderScenario;
		public final ForgeConfigSpec.BooleanValue enableTip;
		public final ForgeConfigSpec.DoubleValue fogDensity;
		public final ForgeConfigSpec.IntValue fogColorDay;
		public final ForgeConfigSpec.IntValue fogColorNight;
		public final ForgeConfigSpec.BooleanValue weatherRenderChanges;
		public final ForgeConfigSpec.IntValue snowDensity;
		public final ForgeConfigSpec.IntValue blizzardDensity;
		public final ForgeConfigSpec.BooleanValue snowSounds;
		public final ForgeConfigSpec.BooleanValue windSounds;
		public final ForgeConfigSpec.BooleanValue skyRenderChanges;
		public final ForgeConfigSpec.IntValue scenarioRenderQuality;
		public final ForgeConfigSpec.IntValue scenarioRenderThread;
		public final ForgeConfigSpec.IntValue infraredViewUBOOffset;
		public final ForgeConfigSpec.IntValue wheelMenuRadius;
		public final ForgeConfigSpec.IntValue themeColor;
		public final ForgeConfigSpec.BooleanValue enableWheelMenuCursor;
		public final ForgeConfigSpec.BooleanValue enableTooltips;
		public final ForgeConfigSpec.BooleanValue enableShaderPackCompat;

		Client(ForgeConfigSpec.Builder builder) {
			builder.push("Frosted HUD");
			enableUI = builder
				.comment("Enables The Winter Rescue HUD. THIS IS MODPACK CORE FEATURE, DISABLING IS NOT RECOMMENDED. ")
				.define("enableHUD", true);
			enablesTemperatureOrb = builder
				.comment("Enables the temperature orb overlay. ")
				.define("enableTemperatureOrb", true);
			useFahrenheit = builder.comment("Use Fahrenheit temperature instead of celsus.")
				.define("useFahrenheit", false);
			tempOrbPosition = builder
				.comment("Position of the temperature orb in game screen. ")
				.defineEnum("renderTempOrbAtCenter", TempOrbPos.MIDDLE);
			tempOrbOffsetX = builder
				.comment(
					"X Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used. ")
				.defineInRange("tempOrbOffsetX", 0, -4096, 4096);
			tempOrbOffsetY = builder
				.comment(
					"Y Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used.  ")
				.defineInRange("tempOrbOffsetY", 0, -4096, 4096);
			enableWaypoint = builder
				.comment("Enables the waypoints rendering. ")
				.define("enableWaypoint", true);
			enableTip = builder.comment("Enables the tips rendering. ")
				.define("enableTip", true);
			wheelMenuRadius = builder
				.comment("Radius of the Radial Menu. ")
				.defineInRange("wheelMenuRadius", 100, 60, Integer.MAX_VALUE);
			enableWheelMenuCursor = builder
				.comment("Enables the cursor in the Radial Menu. ")
				.define("enableWheelMenuCursor", false);
			themeColor = builder
				.comment("The theme color of most FH HUDs. ")
				.defineInRange("themeColor", Colors.CYAN, Integer.MIN_VALUE, Integer.MAX_VALUE);
			builder.pop();

			builder.push("Frozen Effects");
			enableFrozenOverlay = builder
				.comment("Enables the frozen overlay when player is freezing. ")
				.define("enableFrozenOverlay", true);
			enableFrozenVignette = builder
				.comment("Enables the vignette when player is freezing. ")
				.define("enableFrozenVignette", true);
			enableHeatVignette = builder
				.comment("Enables the vignette when player is too hot. ")
				.define("enableHeatVignette", true);
			enableBreathParticle = builder
				.comment("Enables the breath particle when environment is cold. ")
				.define("enableBreathParticle", true);
			enableFrozenSound = builder
				.comment("Enables the frozen sound when player is freezing. ")
				.define("enableFrozenSound", true);
			infraredViewUBOOffset = builder.comment("The binding offset of the UBO for the infrared view shader.")
				.comment("Partial shaders and mods may occupy the position as well.")
				.comment("We will use default offset (7) for some known mods here. However, it is not guaranteed to be always compatible with all mods / shaders.")
				.comment("In this case, player have to modify the config to specify the offset.")
				.comment("No worries, from my experience, offset 7 is compatible with 99% mods / shaders.")
				.defineInRange("infraredViewUBOOffset", 7, 0, Integer.MAX_VALUE);
			builder.pop();

			builder.push("Weather");
			weatherRenderChanges = builder.comment("Enables weather rendering changes.")
				.define("weatherRenderChanges", true);
			fogDensity = builder.comment("How dense the fog effect during a snowstorm is.")
				.defineInRange("fogDensity", 0.1, 0, 1);
			fogColorDay = builder.comment("This is the fog color during the day. It must be an RGB hex string.")
				.defineInRange("fogColorDay", 0xbfbfd8, 0x000000, 0xffffff);
			fogColorNight = builder.comment("This is the fog color during the night. It must be an RGB hex string.")
				.defineInRange("fogColorNight", 0x0c0c19, 0x000000, 0xffffff);
			snowDensity = builder
				.comment("How visually dense the snow weather effect is. Normally, vanilla sets this to 5 with fast graphics, and 10 with fancy graphics.")
				.defineInRange("snowDensity", 10, 1, 15);
			blizzardDensity = builder
				.comment("How visually dense the snow weather effect is. Normally, vanilla sets this to 5 with fast graphics, and 10 with fancy graphics.")
				.defineInRange("blizzardDensity", 15, 1, 15);
			snowSounds = builder
				.comment("Enable snow weather sounds.")
				.define("snowSounds", true);
			windSounds = builder
				.comment("Enable blizzard wind weather sounds.")
				.define("windSounds", true);
			skyRenderChanges = builder
				.comment("Changes the sky renderer to one which does not render sunrise or sunset effects during a snowstorm.")
				.define("skyRenderChanges", true);
			builder.pop();

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

			builder.pop();

			builder.push("other");
			enableTooltips = builder.comment("Enable item tooltips")
				.define("enableTooltips", true);
			enableShaderPackCompat = builder.comment("Enables shaderpack compatibility module, switch this off if your shader does not load correctly")
				.define("enableShaderCompatibility", true);
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

		public final ForgeConfigSpec.ConfigValue<List<? extends String>> blackmods;

		// public final ForgeConfigSpec.ConfigValue<Boolean> enableAutoRestart;
		public final ForgeConfigSpec.ConfigValue<Boolean> enableUpdateReminder;

		Common(ForgeConfigSpec.Builder builder) {

			builder.push("Miscellaneous");
			blackmods = builder
				.comment("BlackListed mods to kick player")
				.defineList("Mod Blacklist", new ArrayList<>(), s -> true);
			builder.pop();
			builder.push("AutoUpdate");
			/*
			 * enableAutoRestart=builder.
			 * comment("Enable automatic restart if later snapshot was found, DONT TOUCH UNLESS INSTRUCTED BY DEV TEAM."
			 * ) .define("enableAutoRestart", false);
			 */
			enableUpdateReminder = builder.comment("Enable update reminder if later snapshot was found, DONT TOUCH UNLESS INSTRUCTED BY DEV TEAM.")
				.define("enableUpdateReminder", true);
			builder.pop();
		}
	}

	/**
	 * Config value that would sync between client and server
	 * 
	 */
	public static class Server {
		public static class WeatherForecast {
			public final ForgeConfigSpec.BooleanValue enablesTemperatureForecast;
			public final ForgeConfigSpec.BooleanValue forceEnableTemperatureForecast;

			WeatherForecast(ForgeConfigSpec.Builder builder) {
				builder.push("Weather Forecast");
				enablesTemperatureForecast = builder
					.comment("Enables the weather forecast system. ")
					.define("enablesTemperatureForecast", true);
				forceEnableTemperatureForecast = builder
					.comment("Forces the weather forecast system to be enabled regardless of scenario. ")
					.define("forceEnableTemperatureForecast", false);
				builder.pop();
			}
		}

		public static class Climate {

			public final ForgeConfigSpec.EnumValue<FHTemperatureDifficulty> tdiffculty;
			public final ForgeConfigSpec.ConfigValue<Double> tempSpeed;
			// public final ForgeConfigSpec.ConfigValue<Integer> simulationParticleLife;
			public final ForgeConfigSpec.ConfigValue<Integer> temperatureUpdateIntervalTicks;
			public final ForgeConfigSpec.ConfigValue<Integer> wetEffectDuration;
			public final ForgeConfigSpec.ConfigValue<Integer> wetClothesDurationMultiplier;
			public final ForgeConfigSpec.ConfigValue<Integer> tempSkyLightThreshold;
			public final ForgeConfigSpec.ConfigValue<Integer> snowTempModifier;
			public final ForgeConfigSpec.ConfigValue<Integer> blizzardTempModifier;
			public final ForgeConfigSpec.ConfigValue<Integer> dayNightTempAmplitude;
			public final ForgeConfigSpec.ConfigValue<Integer> onFireTempModifier;
			public final ForgeConfigSpec.ConfigValue<Integer> heatExchangeTimeConstant;
			public final ForgeConfigSpec.ConfigValue<Double> heatExchangeTempConstant;
			public final ForgeConfigSpec.BooleanValue addInitClimate;
			public final ForgeConfigSpec.IntValue envTempUpdateIntervalTicks;
			public final ForgeConfigSpec.IntValue envTempThreadCount;
			public final ForgeConfigSpec.IntValue tempBlockstateUpdateIntervalTicks;
			public final ForgeConfigSpec.IntValue ambientBlockStateUpdateDivisor;
			public final ForgeConfigSpec.IntValue tempRandomTickSpeedDivisor;
			public final ForgeConfigSpec.ConfigValue<Integer> blizzardFrequency;
			public final ForgeConfigSpec.ConfigValue<Double> hurtingHeatUpdate;
			public final ForgeConfigSpec.ConfigValue<Integer> minBodyTempChange;
			public final ForgeConfigSpec.ConfigValue<Integer> maxBodyTempChange;
			public final ForgeConfigSpec.IntValue generatorSteamSpeed;
			public final ForgeConfigSpec.IntValue generatorSteamCost;
			Climate(ForgeConfigSpec.Builder builder) {

				builder.push("Temperature");

				tdiffculty = builder.comment("Temperature System difficulty", "easy=Strong body", "normal=Average", "hard=Reality", "hardcore=Sick body")
					.defineEnum("temperatureDifficulty", FHTemperatureDifficulty.normal);
				tempSpeed = builder
					.comment("Modifier of body temperature change speed, Adjust this higher only when you lower the update interval respectively This does not affect hypothermia temperature.")
					.defineInRange("temperatureChangeRate", 1f, 0, 20);
				temperatureUpdateIntervalTicks = builder.comment("The interval of temperature update in ticks.")
					.defineInRange("temperatureUpdateIntervalTicks", 20, 1, Integer.MAX_VALUE);
				envTempUpdateIntervalTicks = builder.comment("The shortest interval of environment(block) temperature update in ticks.")
					.defineInRange("environmentTempMinTicks", 20, 1, Integer.MAX_VALUE);
				tempBlockstateUpdateIntervalTicks = builder.comment("The interval for block state update due to temperature.")
					.defineInRange("tempBlockstateUpdateIntervalTicks", 20, 1, Integer.MAX_VALUE);
				tempRandomTickSpeedDivisor = builder.comment("The random tick speed is divided by this value when used for temperature related updates.")
					.defineInRange("tempRandomTickSpeedDivisor", 1, 1, Integer.MAX_VALUE);
				ambientBlockStateUpdateDivisor = builder.comment("Block update divisor for ambient blocks(blocks without heat area).")
					.defineInRange("ambientRandomTickSpeedDivisor", 10, 1, Integer.MAX_VALUE);
				int numProcessor = Runtime.getRuntime().availableProcessors();
				envTempThreadCount = builder.comment("The number of threads used for environment(block) temperature update, set to 0 disables multithreading, default to min(processors/2,2)")
					.defineInRange("environmentTempMinTicks", Math.min(2, numProcessor / 2), 0, 16);
				wetEffectDuration = builder.comment("The duration of the wet effect applied in water in ticks.")
					.defineInRange("wetEffectDuration", 100, 1, Integer.MAX_VALUE);
				wetClothesDurationMultiplier = builder.comment("The multiplier of the wet effect duration when player is wearing clothes.")
					.comment("finalDuration = wetEffectDuration * wetClothesDurationMultiplier")
					.defineInRange("wetClothesDurationMultiplier", 4, 1, 1000);
				tempSkyLightThreshold = builder.comment("Below which -dayNightTempModifier will be used.")
					.defineInRange("tempSkyLightThreshold", 5, 0, 15);
				snowTempModifier = builder.comment("The temperature modifier when player is in snow weather.")
					.defineInRange("snowTempModifier", -5, -100, 100);
				blizzardTempModifier = builder.comment("The temperature modifier when player is in blizzard weather.")
					.defineInRange("blizzardTempModifier", -10, -100, 100);
				dayNightTempAmplitude = builder.comment("This is the amplitude of day night temperature cycle.")
					.comment("The actual temperature modifier is sin(time) * dayNightTempAmplitude.")
					.comment("Note that when sky light is below tempSkyLightThreshold, the modifier will be dayNightTempAmplitude * -1.")
					.comment("Note that when snow or blizzard occurs, amplitude is reduced to 1/5 as sunlight is blocked.")
					.comment("Ref: https://en.wikipedia.org/wiki/Diurnal_air_temperature_variation")
					.comment("Such amplitude could be up to 50 Celsius in extreme.")
					.comment("More humid, more stable. More dry, more extreme.")
					.comment("We set default to be 10, as arctic is quite stable.")
					.defineInRange("dayNightTempAmplitude", 10, -100, 100);
				onFireTempModifier = builder.comment("The temperature modifier when player is on fire.")
					.defineInRange("onFireTempModifier", 150, 0, 1000);
				heatExchangeTimeConstant = builder.comment("The heat exchange time constant between player and environment.")
					.comment("Definition: The value has unit in seconds.")
					.comment("It represents the theoretical time it takes for a naked player without self-heating")
					.comment("to reach the mildest hypothermia (36C body temperature)")
					.comment("when exposed to an effective environment temperature of heatExchangeTempConstant below 37C.")
					.defineInRange("heatExchangeTimeConstant", 1000, 0, Integer.MAX_VALUE);
				heatExchangeTempConstant = builder.comment("The heat exchange temperature constant between player and environment.")
					.comment("Check the comment on heatExchangeTimeConstant for what is this.")
					.defineInRange("heatExchangeTempConstant", 10D, 0D, Integer.MAX_VALUE);
				hurtingHeatUpdate = builder.comment("The heat update when player is hurt.")
					.defineInRange("hurtingHeatUpdate", 0.1, 0, 1);
				minBodyTempChange = builder.comment("The minimum body temperature change relative to 37.")
					.defineInRange("minBodyTempChange", -10, -100, 100);
				maxBodyTempChange = builder.comment("The maximum body temperature change relative to 37.")
					.defineInRange("maxBodyTempChange", 10, -100, 100);
				addInitClimate = builder.comment("Whether should an initial climate event added to newly created world: a snowstorm after three days")
					.define("addInitClimate", true);
				blizzardFrequency = builder.comment("Frequency out of 10 a blizzard happens when a new climate event happens.")
					.defineInRange("blizzardFrequency", 3, 0, 10);
				
				builder.pop();
				builder.push("Generator");
				generatorSteamSpeed = builder.comment("Generator steam input rate mb/t")
					.defineInRange("generatorSteamRate", 144, 1, 1000);
				generatorSteamCost = builder.comment("Generator steam input time tick per 1%")
					.defineInRange("generatorSteamTick", 60, 1, Integer.MAX_VALUE);
				builder.pop();

			}
		}

		public static class SteamCore {
			public final ForgeConfigSpec.ConfigValue<Double> steamCoreMaxPower;
			public final ForgeConfigSpec.ConfigValue<Double> steamCorePowerIntake;
			public final ForgeConfigSpec.ConfigValue<Double> steamCoreGeneratedSpeed;
			public final ForgeConfigSpec.ConfigValue<Double> steamCoreCapacity;

			SteamCore(ForgeConfigSpec.Builder builder) {
				builder.push("Steam Core");
				steamCoreMaxPower = builder.comment("The max power which steam core can store.Steam Core will cost the power stored without any heat source connected.")
					.defineInRange("steamCoreMaxPower", 600f, 100f, 6000000f);
				steamCorePowerIntake = builder.comment("SteamCore will cost such heat 20 times per second.")
					.defineInRange("steamCorePowerIntake", 8f, 0f, 6000000f);
				steamCoreGeneratedSpeed = builder.comment("The speed which steam core can provide.")
					.defineInRange("steamCoreGeneratedSpeed", 32f, 0f, 256f);
				steamCoreCapacity = builder.comment("The capacity which steam core can provide.")
					.defineInRange("steamCoreCapacity", 32, 0f, 256f);
				builder.pop();

			}
		}

		public static class VAWT {
			public final ForgeConfigSpec.ConfigValue<Float> vawtDurability;
			public final ForgeConfigSpec.ConfigValue<Double> vawtCapacity;
			public final ForgeConfigSpec.IntValue vawtEmptyAreaRange;
			public final ForgeConfigSpec.IntValue vawtEmptyAreaAllowsBlockCount;
			public final ForgeConfigSpec.ConfigValue<Integer> vawtEmptyAreaMaxDetectCooldown;

			VAWT(ForgeConfigSpec.Builder builder) {
				builder.push("VAWT");
				vawtDurability = builder.comment("""
								The durability coefficient of VAWT.
								It will not affect the VAWTs that was previously placed.
								""")
						.define("vawtDurability", 1f);
				vawtCapacity = builder.comment("The capacity which VAWT can provide.")
						.defineInRange("vawtCapacity", 9f, 0f, 256f);
				vawtEmptyAreaRange = builder.comment("Detection radius of the open area.")
						.defineInRange("vawtEmptyAreaRange", 8, 1, 64);
				vawtEmptyAreaAllowsBlockCount = builder.comment("")
						.defineInRange("vawtEmptyAreaAllowsBlockCount", 32, 0, 1024);
				vawtEmptyAreaMaxDetectCooldown = builder.comment("Try increase this if you have performance issue by placing too many VAWTs. Unit: Second")
						.define("vawtEmptyAreaMaxDetectCooldown", 60);
				builder.pop();
			}
		}

		public static class Nutrition {
			public final ForgeConfigSpec.ConfigValue<Double> waterReducingRate;
			public final ForgeConfigSpec.IntValue weaknessEffectAmplifier;
			public final ForgeConfigSpec.BooleanValue resetWaterLevelInDeath;
			public final ForgeConfigSpec.ConfigValue<Double> nutritionConsumptionRate;
			public final ForgeConfigSpec.ConfigValue<Double> nutritionGainRate;

			Nutrition(ForgeConfigSpec.Builder builder) {
				builder.push("Water & Nutrition");
				waterReducingRate = builder.comment("finalReducingValue = basicValue * waterReducingRate.(DoubleValue)")
					.defineInRange("waterReducingRate", 1.0D, 0d, 1000D);
				weaknessEffectAmplifier = builder
					.comment("It is the weakness effect amplifier of the effect punishment when player's water level is too low. -1 means canceling this effect. Default:0")
					.defineInRange("weaknessEffectAmplifier", 0, -1, 999999);
				resetWaterLevelInDeath = builder.comment("It decides if players' water level would reset in death.")
					.define("resetWaterLevelInDeath", true);
				nutritionConsumptionRate = builder.comment("The rate of nutrition consumption.")
					.defineInRange("nutritionConsumptionRate", 0.0025, 0, 10);
				nutritionGainRate = builder.comment("The rate of nutrition gain by eating food.")
					.defineInRange("nutritionGainRate", 0.0025, 0, 100);
				builder.pop();

			}
		}

		public static class WorldGen {
			public final ForgeConfigSpec.BooleanValue enableSnowAccumulationDuringWeather;
			public final ForgeConfigSpec.IntValue snowAccumulationDifficulty;
			public final ForgeConfigSpec.ConfigValue<List<? extends String>> nonWinterBiomes;
			public final ForgeConfigSpec.BooleanValue invertNonWinterBiomes;
			public final ForgeConfigSpec.BooleanValue enableSnowAccumulationDuringWorldgen;

			WorldGen(ForgeConfigSpec.Builder builder) {
				builder.push("Worldgen");
				enableSnowAccumulationDuringWeather = builder.comment("Enables snow accumulation during snow weather.")
					.define("enableSnowAccumulationDuringWeather", true);
				snowAccumulationDifficulty = builder.comment("The the inverse of this value is the probability of snow adding one layer during each tick.")
					.defineInRange("snowAccumulationDifficulty", 16, 1, Integer.MAX_VALUE);
				nonWinterBiomes = builder.comment("Biomes that are not considered winter biomes.")
					.define("nonWinterBiomes", List.of(
						Biomes.NETHER_WASTES.location().toString(),
						Biomes.CRIMSON_FOREST.location().toString(),
						Biomes.WARPED_FOREST.location().toString(),
						Biomes.BASALT_DELTAS.location().toString(),
						Biomes.SOUL_SAND_VALLEY.location().toString(),
						Biomes.END_BARRENS.location().toString(),
						Biomes.END_HIGHLANDS.location().toString(),
						Biomes.END_MIDLANDS.location().toString(),
						Biomes.THE_END.location().toString(),
						Biomes.THE_VOID.location().toString(),
						// oceans freeze already in their own style
						Biomes.FROZEN_OCEAN.location().toString(),
						Biomes.DEEP_FROZEN_OCEAN.location().toString()));
				invertNonWinterBiomes = builder.comment("If true, the 'nonWinterBiomes' config option will be interpreted as a list of winter biomes, and all others will be ignored.")
					.define("invertNonWinterBiomes", false);
				enableSnowAccumulationDuringWorldgen = builder.comment("Enables snow accumulation during world generation.")
					.define("enableSnowAccumulationDuringWorldgen", false);
				builder.pop();

			}
		}

		public static class FireIgnition {
			public final ForgeConfigSpec.ConfigValue<Double> flintIgnitionChance;
			public final ForgeConfigSpec.ConfigValue<Double> stickIgnitionChance;
			public final ForgeConfigSpec.ConfigValue<Double> consumeChanceWhenIgnited;

			FireIgnition(ForgeConfigSpec.Builder builder) {
				builder.push("Fire Ignition");
				flintIgnitionChance = builder.comment("The chance of igniting when using a flint and metal.")
					.defineInRange("flintIgnitionChance", 0.1, 0, 1);
				stickIgnitionChance = builder.comment("The chance of igniting igniting when using a stick.")
					.defineInRange("stickIgnitionChance", 0.05, 0, 1);
				consumeChanceWhenIgnited = builder.comment("The chance of consuming the item when ignited.")
					.defineInRange("consumeChanceWhenIgnited", 0.1, 0, 1);
				builder.pop();

			}
		}

		public static class TemperatureSimulation {
			public final ForgeConfigSpec.ConfigValue<Integer> simulationRange;
			public final ForgeConfigSpec.ConfigValue<Integer> simulationDivision;
			public final ForgeConfigSpec.ConfigValue<Double> simulationParticleInitialSpeed;

			TemperatureSimulation(ForgeConfigSpec.Builder builder) {
				builder.push("Surrounding Temperature Simulation").comment("The simulator is used to simulate the temperature of the surrounding environment. Not recommended to change.");
				simulationRange = builder.comment("The range of the simulation.")
					.defineInRange("simulationRange", 8, 1, 8);
				simulationDivision = builder.comment("The number of divisions of unit square in the simulation.")
					.comment("Number of particles is cubic of this value.")
					.comment("If your server lags, you can reduce this value.")
					.defineInRange("simulationDivision", 10, 1, 100);
				simulationParticleInitialSpeed = builder.comment("The initial speed of the particles in the simulation.")
					.defineInRange("simulationParticleInitialSpeed", 0.4f, 0.01f, 1f);
				// simulationParticleLife = builder.comment("The life ticks of the particles in
				// the simulation.")
				// .defineInRange("simulationParticleLife", 20, 1, 100);
				builder.pop();

			}
		}

		public static class Town {
			public final ForgeConfigSpec.BooleanValue enableTownTick;
			public final ForgeConfigSpec.BooleanValue enableTownTickMorning;
			public static class Resource{
				public final ForgeConfigSpec.ConfigValue<Double> oreCount;
				public final ForgeConfigSpec.ConfigValue<Double> oreRecovery;
				public final ForgeConfigSpec.ConfigValue<Double> treeCount;
				public final ForgeConfigSpec.ConfigValue<Double> treeRecovery;
				public final ForgeConfigSpec.ConfigValue<Double> huntCount;
				public final ForgeConfigSpec.ConfigValue<Double> huntRecovery;
				public final ForgeConfigSpec.ConfigValue<Double> poiCount;
				public final ForgeConfigSpec.ConfigValue<Double> poiRecovery;
				public final ForgeConfigSpec.ConfigValue<Double> salvageCount;
				public final ForgeConfigSpec.ConfigValue<Double> salvageRecovery;
				Resource(ForgeConfigSpec.Builder builder) {
					builder.push("Pick Resource");
					oreCount=builder.comment("Ore Count per block squared")
						.defineInRange("orePerSq", 15d, 0d, 1000000d);
					oreRecovery=builder.comment("Ore Recovery per block per day")
						.defineInRange("orePerDay", 0d, 0d, 1000000d);
					treeCount=builder.comment("Tree Count per block squared")
						.defineInRange("treePerSq", 0.4d, 0d, 1000000d);
					treeRecovery=builder.comment("Tree Recovery per block per day")
						.defineInRange("treePerDay", 0.0025d, 0d, 1000000d);
					huntCount=builder.comment("Hunt Count per block squared")
						.defineInRange("huntPerSq", 0.1d, 0d, 1000000d);
					huntRecovery=builder.comment("Hunt Recovery per block per day")
						.defineInRange("huntPerDay", 0.005d, 0d, 1000000d);
					poiCount=builder.comment("Research Point Count per block squared")
						.defineInRange("poiPerSq", 100d, 0d, 1000000d);
					poiRecovery=builder.comment("Research Point Recovery per block per day")
						.defineInRange("poiPerDay", 0.5d, 0d, 1000000d);
					salvageCount=builder.comment("Salvage Count per block squared")
						.defineInRange("salvagePerSq", 0.25d, 0d, 1000000d);
					salvageRecovery=builder.comment("Salvage Recovery per block per day")
						.defineInRange("salvagePerDay", 0.05d, 0d, 1000000d);
					builder.pop();
				}
			}
			public final Resource RESOURCE;
			Town(ForgeConfigSpec.Builder builder) {
				builder.push("Town");
				enableTownTick = builder.comment("Enables town tick every second.")
					.comment("This tick includes the running of town worker blocks.")
					.define("enableTownTick", true);
				enableTownTickMorning = builder.comment("Enables town tick in the morning of each days.")
					.comment("This tick includes the refresh of some town things, like house allocating, checking overlap of buildings, work assigning...")
					.define("enableTownTickMorning", true);
				RESOURCE=new Resource(builder);
				builder.pop();

			}
		}

		public static class Misc {

			public final ForgeConfigSpec.BooleanValue alwaysKeepInventory;
			public final ForgeConfigSpec.BooleanValue keepEquipments;
			public final ForgeConfigSpec.BooleanValue fixEssJeiIssue;
			public final ForgeConfigSpec.ConfigValue<List<? extends String>> developers;
			public final ForgeConfigSpec.ConfigValue<Boolean> enablePlayerPooping;
			public final ForgeConfigSpec.BooleanValue enableDailyKitchen;
			public final ForgeConfigSpec.ConfigValue<Boolean> enableScenario;

			Misc(ForgeConfigSpec.Builder builder) {
				builder.push("Miscellaneous");
				alwaysKeepInventory = builder
					.comment("Always keep inventory on death on every dimension and world")
					.define("alwaysKeepInventory", false);
				keepEquipments = builder.comment("Instead of keeping all inventory, only keep equipments, curios and quickbar tools on death")
					.define("keepEquipments", true);
				fixEssJeiIssue = builder
					.comment("Fixes JEI and Bukkit server compat issue, don't touch unless you know what you are doing.")
					.define("fixEssJeiIssue", true);
				developers = builder
					.comment("Special array of players")
					.defineList("Player Whitelist", DEFAULT_WHITELIST, s -> true);
				enablePlayerPooping = builder
					.comment("Enables the pooping mechanic through shifting.")
					.define("enablePlayerPooping", true);
				enableDailyKitchen = builder
					.comment("Enables sending wanted food message. ")
					.define("enableDailyKitchen", true);
				enableScenario = builder
					.comment("Enables the scenario system. ")
					.define("enableScenario", true);
				builder.pop();
			}
		}

		public final WeatherForecast WEATHER_FORECAST;
		public final Climate CLIMATE;
		public final SteamCore STEAM_CORE;
		public final VAWT VAWT;
		public final Nutrition NUTRITION;
		public final WorldGen WORLDGEN;
		public final FireIgnition FIRE_IGNITION;
		public final TemperatureSimulation SIMULATION;
		public final Town TOWN;
		public final Misc MISC;

		Server(ForgeConfigSpec.Builder builder) {
			WEATHER_FORECAST = new WeatherForecast(builder);
			CLIMATE = new Climate(builder);
			STEAM_CORE = new SteamCore(builder);
			VAWT = new VAWT(builder);
			NUTRITION = new Nutrition(builder);
			WORLDGEN = new WorldGen(builder);
			FIRE_IGNITION = new FireIgnition(builder);
			SIMULATION = new TemperatureSimulation(builder);
			TOWN = new Town(builder);
			MISC = new Misc(builder);
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

		DEFAULT_WHITELIST.add("YueSha");
		DEFAULT_WHITELIST.add("duck_egg");
		DEFAULT_WHITELIST.add("Evan");
		DEFAULT_WHITELIST.add("dashuaibia");
		DEFAULT_WHITELIST.add("khjxiaogu");
		DEFAULT_WHITELIST.add("Lyuuke");
		DEFAULT_WHITELIST.add("goumo_g");
		DEFAULT_WHITELIST.add("alphaGem");
		DEFAULT_WHITELIST.add("JackyWang");
		DEFAULT_WHITELIST.add("Fu_Yang");
		DEFAULT_WHITELIST.add("asdfghjkl");
		DEFAULT_WHITELIST.add("03110");
		DEFAULT_WHITELIST.add("shidi");
		DEFAULT_WHITELIST.add("yuqijun");
		DEFAULT_WHITELIST.add("Dsanilen");
		DEFAULT_WHITELIST.add("Lanshan");
		DEFAULT_WHITELIST.add("Dev");
	}

	public static void register() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, FHConfig.CLIENT_CONFIG);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FHConfig.COMMON_CONFIG);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FHConfig.SERVER_CONFIG);
	}

	public static boolean isWinterBiome(ResourceLocation name) {
		if (name != null) {
			final Stream<ResourceLocation> stream = FHConfig.SERVER.WORLDGEN.nonWinterBiomes.get().stream().map(ResourceLocation::new);
			return FHConfig.SERVER.WORLDGEN.invertNonWinterBiomes.get() ? stream.anyMatch(name::equals) : stream.noneMatch(name::equals);
		}
		return false;
	}
}
