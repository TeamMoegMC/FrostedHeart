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

package com.teammoeg.frostedheart.infrastructure.config;

import com.teammoeg.chorda.client.cui.screenadapter.OverlayPositioner;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateEventModel;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClockSource;
import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.time.MonthDay;
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
		public final ForgeConfigSpec.BooleanValue enableTownEventTips;
		public final ForgeConfigSpec.EnumValue<OverlayPositioner.All> tipPosition;
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
		public final ForgeConfigSpec.BooleanValue scenarioAntiAliasing;
		public final ForgeConfigSpec.IntValue infraredViewUBOOffset;
		public final ForgeConfigSpec.IntValue wheelMenuRadius;
		public final ForgeConfigSpec.IntValue themeColor;
		public final ForgeConfigSpec.BooleanValue enableWheelMenuCursor;
		public final ForgeConfigSpec.BooleanValue enableTooltips;
		public final ForgeConfigSpec.BooleanValue enableKeyHints;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledHints;
		public final ForgeConfigSpec.EnumValue<OverlayPositioner.All> hintPosition;

		Client(ForgeConfigSpec.Builder builder) {
			builder.push("Frosted HUD");
			enableUI = builder
				.comment("Enables The Winter Rescue HUD. THIS IS MODPACK CORE FEATURE, DISABLING IS NOT RECOMMENDED. ")
				.define("enableHUD", true);
			themeColor = builder
					.comment("The theme color of most FH HUDs. ")
					.defineInRange("themeColor", Colors.CYAN, Integer.MIN_VALUE, Integer.MAX_VALUE);
			builder.push("Temperature Orb");
				enablesTemperatureOrb = builder
						.comment("Enables the temperature orb overlay. ")
						.define("enableTemperatureOrb", true);
				useFahrenheit = builder.comment("Use Fahrenheit temperature instead of celsus.")
						.define("useFahrenheit", false);
				tempOrbPosition = builder
						.comment("Position of the temperature orb in game screen. ")
						.defineEnum("renderTempOrbAtCenter", TempOrbPos.MIDDLE);
				tempOrbOffsetX = builder
						.comment("X Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used. ")
						.defineInRange("tempOrbOffsetX", 0, -4096, 4096);
				tempOrbOffsetY = builder
						.comment("Y Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used.  ")
						.defineInRange("tempOrbOffsetY", 0, -4096, 4096);
			builder.pop();
			builder.push("Wheel Menu");
				wheelMenuRadius = builder
						.comment("Radius of the Radial Menu. ")
						.defineInRange("wheelMenuRadius", 100, 60, Integer.MAX_VALUE);
				enableWheelMenuCursor = builder
						.comment("Enables the cursor in the Radial Menu. ")
						.define("enableWheelMenuCursor", false);
			builder.pop();
			builder.push("Tip");
				enableTip = builder.comment("Enables the tips rendering. ")
						.define("enableTip", true);
				enableTownEventTips = builder.comment("Enables transient town event tips. ")
						.define("enableTownEventTips", true);
				tipPosition = builder.comment("The position where the tip display")
						.defineEnum("tipPosition", OverlayPositioner.All.MIDDLE_RIGHT);
			builder.pop();
			builder.push("Waypoint");
				enableWaypoint = builder
						.comment("Enables the waypoints rendering. ")
						.define("enableWaypoint", true);
			builder.pop();
			builder.push("Key Hint");
			enableKeyHints = builder.comment("Enable key hints")
					.define("enableKeyHints", true);
			disabledHints = builder.comment("Disable the hints you don't want to display")
					.comment("Example: 'frostedheart:example'")
					.defineList("disabledHints", List.of(), o -> true);
			hintPosition = builder
					.comment("The position where the hint display")
					.defineEnum("hintPosition", OverlayPositioner.All.MIDDLE_LEFT);
			builder.pop();
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
			scenarioAntiAliasing = builder.comment("Scenario rendering Antialiasing, turn off to higher performance")
				.define("scenarioAntiAliasing", true);
			builder.pop();



			builder.push("other");
			enableTooltips = builder.comment("Enable item tooltips")
				.define("enableTooltips", true);
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
			public final ForgeConfigSpec.IntValue longTermTrackCount;
			public final ForgeConfigSpec.IntValue eventChoiceRollBound;
			public final ForgeConfigSpec.IntValue warmEventMinimumRollInclusive;
			public final ForgeConfigSpec.IntValue openingWarmRollBonus;
			public final ForgeConfigSpec.IntValue openingBiasThroughDayInclusive;
			public final ForgeConfigSpec.DoubleValue coldBottomExtremeCelsius;
			public final ForgeConfigSpec.DoubleValue coldBottomSevereCelsius;
			public final ForgeConfigSpec.DoubleValue coldBottomStrongCelsius;
			public final ForgeConfigSpec.DoubleValue coldBottomNormalCelsius;
			public final ForgeConfigSpec.IntValue coldBottomWeightExtreme;
			public final ForgeConfigSpec.IntValue coldBottomWeightSevere;
			public final ForgeConfigSpec.IntValue coldBottomWeightStrong;
			public final ForgeConfigSpec.IntValue coldBottomWeightNormal;
			public final ForgeConfigSpec.IntValue climateEventMinimumDays;
			public final ForgeConfigSpec.IntValue climateEventMaximumDaysExclusive;
			public final ForgeConfigSpec.IntValue climatePaddingMinimumHours;
			public final ForgeConfigSpec.IntValue climatePaddingMaximumHoursExclusive;
			public final ForgeConfigSpec.IntValue climateCalmMinimumDays;
			public final ForgeConfigSpec.IntValue climateCalmMaximumDaysExclusive;
			public final ForgeConfigSpec.DoubleValue coldPreludePeakCelsius;
			public final ForgeConfigSpec.DoubleValue warmPeakCelsius;
			public final ForgeConfigSpec.DoubleValue forecastSensitivityCelsius;
			public final ForgeConfigSpec.DoubleValue climateEventNoiseStandardDeviationCelsius;
			public final ForgeConfigSpec.DoubleValue warmEventNoiseScale;
			public final ForgeConfigSpec.IntValue climateStoneInterfaceLevel;
			public final ForgeConfigSpec.IntValue climateSeaLevel;
			public final ForgeConfigSpec.DoubleValue blockMaximumClimateAffection;
			public final ForgeConfigSpec.DoubleValue blockHeatApplicationMultiplier;
			public final ForgeConfigSpec.DoubleValue absoluteZeroCelsius;
			public final ForgeConfigSpec.DoubleValue overworldBaselineCelsius;
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

				builder.push("Long Term Events");
				longTermTrackCount = builder
					.comment("Number of independent ordinary climate-event tracks combined by max-positive plus min-negative temperature.")
					.defineInRange("trackCount", TownModelParameters.Defaults.CLIMATE_TRACK_COUNT, 1, 16);
				eventChoiceRollBound = builder
					.comment("Exclusive bound of the integer roll selecting a cold or warm event.")
					.defineInRange("eventChoiceRollBound", TownModelParameters.Defaults.CLIMATE_EVENT_CHOICE_ROLL_BOUND, 1, 1000000);
				warmEventMinimumRollInclusive = builder
					.comment("A climate event is warm when the selection roll plus opening bonus reaches this value.")
					.defineInRange("warmEventMinimumRollInclusive", TownModelParameters.Defaults.CLIMATE_WARM_EVENT_MINIMUM_ROLL_INCLUSIVE, 0, 1000000);
				openingWarmRollBonus = builder
					.comment("Warm-selection bonus during the opening-bias period. Long simulations burn in past this period.")
					.defineInRange("openingWarmRollBonus", TownModelParameters.Defaults.CLIMATE_OPENING_WARM_ROLL_BONUS, 0, 1000000);
				openingBiasThroughDayInclusive = builder
					.comment("Last inclusive world day receiving the opening warm-selection bonus.")
					.defineInRange("openingBiasThroughDayInclusive", TownModelParameters.Defaults.CLIMATE_OPENING_BIAS_THROUGH_DAY_INCLUSIVE, 0, 1000000);
				coldBottomExtremeCelsius = builder.defineInRange("coldBottomExtremeCelsius", (double) TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_EXTREME_CELSIUS, -273.0, 1000.0);
				coldBottomSevereCelsius = builder.defineInRange("coldBottomSevereCelsius", (double) TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_SEVERE_CELSIUS, -273.0, 1000.0);
				coldBottomStrongCelsius = builder.defineInRange("coldBottomStrongCelsius", (double) TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_STRONG_CELSIUS, -273.0, 1000.0);
				coldBottomNormalCelsius = builder.defineInRange("coldBottomNormalCelsius", (double) TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_NORMAL_CELSIUS, -273.0, 1000.0);
				coldBottomWeightExtreme = builder.defineInRange("coldBottomWeightExtreme", TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_EXTREME, 1, 1000000);
				coldBottomWeightSevere = builder.defineInRange("coldBottomWeightSevere", TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_SEVERE, 1, 1000000);
				coldBottomWeightStrong = builder.defineInRange("coldBottomWeightStrong", TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_STRONG, 1, 1000000);
				coldBottomWeightNormal = builder.defineInRange("coldBottomWeightNormal", TownModelParameters.Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_NORMAL, 1, 1000000);
				climateEventMinimumDays = builder.defineInRange("eventMinimumDays", TownModelParameters.Defaults.CLIMATE_EVENT_MINIMUM_DAYS, 1, 1000000);
				climateEventMaximumDaysExclusive = builder.defineInRange("eventMaximumDaysExclusive", TownModelParameters.Defaults.CLIMATE_EVENT_MAXIMUM_DAYS_EXCLUSIVE, 2, 1000000);
				climatePaddingMinimumHours = builder.defineInRange("paddingMinimumHours", TownModelParameters.Defaults.CLIMATE_PADDING_MINIMUM_HOURS, 0, 1000000);
				climatePaddingMaximumHoursExclusive = builder.defineInRange("paddingMaximumHoursExclusive", TownModelParameters.Defaults.CLIMATE_PADDING_MAXIMUM_HOURS_EXCLUSIVE, 1, 1000000);
				climateCalmMinimumDays = builder.defineInRange("calmMinimumDays", TownModelParameters.Defaults.CLIMATE_CALM_MINIMUM_DAYS, 1, 1000000);
				climateCalmMaximumDaysExclusive = builder.defineInRange("calmMaximumDaysExclusive", TownModelParameters.Defaults.CLIMATE_CALM_MAXIMUM_DAYS_EXCLUSIVE, 2, 1000000);
				coldPreludePeakCelsius = builder.defineInRange("coldPreludePeakCelsius", (double) TownModelParameters.Defaults.CLIMATE_COLD_PRELUDE_PEAK_CELSIUS, -273.0, 1000.0);
				warmPeakCelsius = builder.defineInRange("warmPeakCelsius", (double) TownModelParameters.Defaults.CLIMATE_WARM_PEAK_CELSIUS, -273.0, 1000.0);
				forecastSensitivityCelsius = builder
					.comment("Temperature margin used when the player forecast crosses a named cold level.")
					.defineInRange("forecastSensitivityCelsius", (double) TownModelParameters.Defaults.CLIMATE_FORECAST_SENSITIVITY_CELSIUS, 0.0, 100.0);
				climateEventNoiseStandardDeviationCelsius = builder.defineInRange("eventNoiseStandardDeviationCelsius", (double) TownModelParameters.Defaults.CLIMATE_EVENT_NOISE_STANDARD_DEVIATION_CELSIUS, 0.0, 1000.0);
				warmEventNoiseScale = builder.defineInRange("warmNoiseScale", (double) TownModelParameters.Defaults.CLIMATE_WARM_NOISE_SCALE, 0.0, 1000.0);
				climateStoneInterfaceLevel = builder
					.comment("At or below this Y level, climate does not affect block temperature.")
					.defineInRange("stoneInterfaceLevel", TownModelParameters.Defaults.CLIMATE_STONE_INTERFACE_LEVEL, -1000000, 1000000);
				climateSeaLevel = builder
					.comment("Above this Y level, block climate affection reaches its configured maximum.")
					.defineInRange("seaLevel", TownModelParameters.Defaults.CLIMATE_SEA_LEVEL, -1000000, 1000000);
				blockMaximumClimateAffection = builder
					.comment("Maximum alpha multiplying climate temperature in the block-temperature formula.")
					.defineInRange("blockMaximumClimateAffection", (double) TownModelParameters.Defaults.CLIMATE_BLOCK_MAXIMUM_AFFECTION, 0.0, 100.0);
				blockHeatApplicationMultiplier = builder
					.comment("Multiplier applied to the maximum heat-field value before the heat-field ceiling is enforced.")
					.defineInRange("blockHeatApplicationMultiplier", (double) TownModelParameters.Defaults.CLIMATE_BLOCK_HEAT_APPLICATION_MULTIPLIER, 0.0, 100.0);
				absoluteZeroCelsius = builder
					.comment("Lower clamp for temperature calculations, in Celsius.")
					.defineInRange("absoluteZeroCelsius", (double) TownModelParameters.Defaults.CLIMATE_ABSOLUTE_ZERO_CELSIUS, -1000.0, 0.0);
				overworldBaselineCelsius = builder
					.comment("Fallback dimension temperature when no datapack world-temperature value is available.")
					.defineInRange("overworldBaselineCelsius", (double) TownModelParameters.Defaults.CLIMATE_OVERWORLD_BASELINE_CELSIUS, -273.0, 1000.0);
				builder.pop();
				
				builder.pop();
				builder.push("Generator");
				generatorSteamSpeed = builder.comment("Generator steam input rate mb/t")
					.defineInRange("generatorSteamRate", 144, 1, 1000);
				generatorSteamCost = builder.comment("Generator steam input time tick per 1%")
					.defineInRange("generatorSteamTick", 60, 1, Integer.MAX_VALUE);
				builder.pop();

			}

			public ClimateEventModel.Parameters eventModelParameters() {
				return new ClimateEventModel.Parameters(
					WorldClockSource.secondsPerHour,
					WorldClockSource.secondsPerDay,
					eventChoiceRollBound.get(),
					warmEventMinimumRollInclusive.get(),
					openingWarmRollBonus.get(),
					openingBiasThroughDayInclusive.get(),
					coldBottomExtremeCelsius.get().floatValue(),
					coldBottomSevereCelsius.get().floatValue(),
					coldBottomStrongCelsius.get().floatValue(),
					coldBottomNormalCelsius.get().floatValue(),
					coldBottomWeightExtreme.get(), coldBottomWeightSevere.get(),
					coldBottomWeightStrong.get(), coldBottomWeightNormal.get(),
					climateEventMinimumDays.get(), climateEventMaximumDaysExclusive.get(),
					climatePaddingMinimumHours.get(), climatePaddingMaximumHoursExclusive.get(),
					climateCalmMinimumDays.get(), climateCalmMaximumDaysExclusive.get(),
					coldPreludePeakCelsius.get().floatValue(), warmPeakCelsius.get().floatValue(),
					climateEventNoiseStandardDeviationCelsius.get().floatValue(),
					warmEventNoiseScale.get().floatValue());
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
			// 注意：Forge 配置不能存 Float —— TOML 会把它写成 1.0 并以 Double 读回，导致
			// "not correct. Correcting" 死循环。用 Double 存，使用处自行 floatValue()。
			public final ForgeConfigSpec.ConfigValue<Double> vawtDurability;
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
						.define("vawtDurability", 1.0D);
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
			public final ForgeConfigSpec.BooleanValue enableSnowAccumulationDuringWorldgen;

			WorldGen(ForgeConfigSpec.Builder builder) {
				builder.push("Worldgen");
				enableSnowAccumulationDuringWeather = builder.comment("Enables snow accumulation during snow weather.")
					.define("enableSnowAccumulationDuringWeather", true);
				snowAccumulationDifficulty = builder.comment("The the inverse of this value is the probability of snow adding one layer during each tick.")
					.defineInRange("snowAccumulationDifficulty", 16, 1, Integer.MAX_VALUE);
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
			public final ForgeConfigSpec.IntValue townUpdateIntervalGameTicks;
			public final ForgeConfigSpec.IntValue maxVisibleCitizensPerPlayer;
			public final ForgeConfigSpec.IntValue maxVisibleCitizensPerServer;

			public static class Observation {
				public final ForgeConfigSpec.IntValue historyDays;
				public final ForgeConfigSpec.DoubleValue reserveWarningDays;
				public final ForgeConfigSpec.DoubleValue reserveCriticalDays;

				Observation(ForgeConfigSpec.Builder builder) {
					builder.push("Player Observation");
					historyDays = builder
						.comment("Number of town settlement snapshots retained for the Mayor's Seal.")
						.defineInRange("historyDays",
							TownModelParameters.Defaults.TOWN_OBSERVATION_HISTORY_DAYS, 2, 3650);
					reserveWarningDays = builder
						.comment("Food or T1 fuel reserve days below this value are shown as a warning.")
						.defineInRange("reserveWarningDays",
							TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_WARNING_DAYS, 0d, 3650d);
					reserveCriticalDays = builder
						.comment("Food or T1 fuel reserve days below this value are shown as critical.")
						.defineInRange("reserveCriticalDays",
							TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_CRITICAL_DAYS, 0d, 3650d);
					builder.pop();
				}
			}

			public static class GeneratorT1 {
				public final ForgeConfigSpec.DoubleValue baseFuelDurationMultiplier;
				public final ForgeConfigSpec.IntValue baseProcessTicksPerGameTick;
				public final ForgeConfigSpec.IntValue overdriveExtraProcessTicksPerGameTick;
				public final ForgeConfigSpec.IntValue baseRadiusBlocks;
				public final ForgeConfigSpec.IntValue additionalRadiusPerLevelBlocks;
				public final ForgeConfigSpec.IntValue temperaturePerLevelCelsius;

				GeneratorT1(ForgeConfigSpec.Builder builder) {
					builder.push("Generator T1");
					baseFuelDurationMultiplier = builder
						.comment("Multiplier applied to generator-recipe process ticks before research efficiency is added.")
						.comment("The effective duration is decimalFloor(recipeTicks * (this value + research bonus)).")
						.defineInRange("baseFuelDurationMultiplier",
							TownModelParameters.Defaults.GENERATOR_T1_BASE_FUEL_DURATION_MULTIPLIER,
							0.001d, 1000000d);
					baseProcessTicksPerGameTick = builder
						.comment("Fuel process ticks consumed per active game tick in normal operation.")
						.defineInRange("baseProcessTicksPerGameTick",
							TownModelParameters.Defaults.GENERATOR_T1_BASE_PROCESS_TICKS_PER_GAME_TICK,
							1, 1000000);
					overdriveExtraProcessTicksPerGameTick = builder
						.comment("Additional fuel process ticks consumed per active game tick while overdrive is enabled.")
						.defineInRange("overdriveExtraProcessTicksPerGameTick",
							TownModelParameters.Defaults.GENERATOR_T1_OVERDRIVE_EXTRA_PROCESS_TICKS_PER_GAME_TICK,
							0, 1000000);
					baseRadiusBlocks = builder
						.comment("Spherical heat-field radius at range level 1, in blocks.")
						.defineInRange("baseRadiusBlocks",
							TownModelParameters.Defaults.GENERATOR_T1_BASE_RADIUS_BLOCKS,
							0, 1000000);
					additionalRadiusPerLevelBlocks = builder
						.comment("Additional spherical heat-field radius per range level above 1, in blocks.")
						.defineInRange("additionalRadiusPerLevelBlocks",
							TownModelParameters.Defaults.GENERATOR_T1_ADDITIONAL_RADIUS_PER_LEVEL_BLOCKS,
							0, 1000000);
					temperaturePerLevelCelsius = builder
						.comment("Heat-field temperature increase per generator temperature level, in Celsius.")
						.defineInRange("temperaturePerLevelCelsius",
							TownModelParameters.Defaults.GENERATOR_T1_TEMPERATURE_PER_LEVEL_CELSIUS,
							0, 1000000);
					builder.pop();
				}
			}

			public static class Housing {
				public final ForgeConfigSpec.DoubleValue foodConsumptionPerResidentDay;
				public final ForgeConfigSpec.DoubleValue nutritionReferencePerFoodUnit;
				public final ForgeConfigSpec.DoubleValue minimumNutritionRecoveryMultiplier;
				public final ForgeConfigSpec.DoubleValue residentNutritionReserveLossPerDay;
				public final ForgeConfigSpec.DoubleValue residentNutritionGainAtReference;
				public final ForgeConfigSpec.DoubleValue residentNutritionMaximumCoverage;
				public final ForgeConfigSpec.DoubleValue residentNutritionMaximumReserve;
				public final ForgeConfigSpec.DoubleValue residentNutritionInitialReserve;
				public final ForgeConfigSpec.DoubleValue residentNutritionHealthyReserve;
				public final ForgeConfigSpec.DoubleValue residentNutritionSevereReserve;
				public final ForgeConfigSpec.DoubleValue residentNutritionRecoveryDirectWeight;
				public final ForgeConfigSpec.DoubleValue residentNutritionRecoverySupportWeight;
				public final ForgeConfigSpec.DoubleValue residentNutritionDeficiencyGrowthFloor;
				public final ForgeConfigSpec.DoubleValue residentNutritionMaximumGrowthBonus;
				public final ForgeConfigSpec.IntValue residentNutritionMealSelectionChunks;
				public final ForgeConfigSpec.DoubleValue residentNutritionChannelNeedUtilityWeight;
				public final ForgeConfigSpec.DoubleValue residentNutritionConditionNeedUtilityWeight;
				public final ForgeConfigSpec.DoubleValue residentNutritionGrowthNeedUtilityWeight;
				public final ForgeConfigSpec.DoubleValue foodDeficitPenaltyExponent;
				public final ForgeConfigSpec.DoubleValue healthLossAtZeroFoodPerResidentDay;
				public final ForgeConfigSpec.DoubleValue mentalLossAtZeroFoodPerResidentDay;
				public final ForgeConfigSpec.DoubleValue maximumHealthRecoveryPerResidentDay;
				public final ForgeConfigSpec.DoubleValue maximumMentalRecoveryPerResidentDay;
				public final ForgeConfigSpec.DoubleValue temperatureComfortWeight;
				public final ForgeConfigSpec.DoubleValue spaceComfortWeight;
				public final ForgeConfigSpec.DoubleValue decorationComfortWeight;
				public final ForgeConfigSpec.IntValue minimumFloorAreaBlocks;
				public final ForgeConfigSpec.IntValue minimumInteriorVolumeBlocks;
				public final ForgeConfigSpec.DoubleValue minimumTemperatureCelsius;
				public final ForgeConfigSpec.DoubleValue maximumTemperatureCelsius;
				public final ForgeConfigSpec.DoubleValue temperatureFullStressDistanceCelsius;
				public final ForgeConfigSpec.DoubleValue temperatureStressPenaltyExponent;
				public final ForgeConfigSpec.DoubleValue healthLossAtFullTemperatureStressPerResidentDay;
				public final ForgeConfigSpec.DoubleValue mentalLossAtFullTemperatureStressPerResidentDay;
				public final ForgeConfigSpec.DoubleValue floorBlocksPerResident;
				public final ForgeConfigSpec.DoubleValue decorationCountLogOffset;
				public final ForgeConfigSpec.DoubleValue decorationCountLogMultiplier;
				public final ForgeConfigSpec.DoubleValue decorationTypeBaseScore;
				public final ForgeConfigSpec.DoubleValue decorationBaseDemand;
				public final ForgeConfigSpec.DoubleValue decorationFloorBlocksPerDemand;

				Housing(ForgeConfigSpec.Builder builder) {
					builder.push("Housing");
					foodConsumptionPerResidentDay = builder
						.comment("Food-resource units consumed per resident per Minecraft day.")
						.defineInRange("foodConsumptionPerResidentDay",
							TownModelParameters.Defaults.HOUSING_FOOD_PER_RESIDENT_DAY, 0d, 100d);
					nutritionReferencePerFoodUnit = builder
						.comment("Nutrition value per consumed food-resource unit that grants maximum food quality.")
						.defineInRange("nutritionReferencePerFoodUnit",
							TownModelParameters.Defaults.HOUSING_NUTRITION_REFERENCE_PER_FOOD_UNIT,
							1d, 1000000d);
					minimumNutritionRecoveryMultiplier = builder
						.comment("Recovery multiplier provided by food with zero nutrition quality.")
						.defineInRange("minimumNutritionRecoveryMultiplier",
							TownModelParameters.Defaults.HOUSING_MINIMUM_NUTRITION_RECOVERY_MULTIPLIER,
							0d, 1d);
					residentNutritionReserveLossPerDay = builder
						.comment("Points removed from each resident nutrition reserve before the daily meal.")
						.defineInRange("residentNutritionReserveLossPerDay",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_RESERVE_LOSS_PER_DAY,
							0d, 100d);
					residentNutritionGainAtReference = builder
						.comment("Reserve points gained by one full reference-quality daily meal in one channel.")
						.defineInRange("residentNutritionGainAtReference",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_GAIN_AT_REFERENCE,
							0d, 100d);
					residentNutritionMaximumCoverage = builder
						.comment("Maximum per-channel meal coverage used for reserve gain.")
						.defineInRange("residentNutritionMaximumCoverage",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_MAXIMUM_COVERAGE,
							0d, 100d);
					residentNutritionMaximumReserve = builder
						.comment("Maximum value stored in each resident nutrition channel.")
						.defineInRange("residentNutritionMaximumReserve",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_MAXIMUM_RESERVE, 1d, 100d);
					residentNutritionInitialReserve = builder
						.comment("Initial value assigned to each nutrition channel of a new resident.")
						.defineInRange("residentNutritionInitialReserve",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_INITIAL_RESERVE, 0d, 100d);
					residentNutritionHealthyReserve = builder
						.comment("Reserve value that represents full availability for recovery and ordinary growth.")
						.defineInRange("residentNutritionHealthyReserve",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_HEALTHY_RESERVE, 0.001d, 100d);
					residentNutritionSevereReserve = builder
						.comment("A nutrition channel below this reserve emits a severe-deficiency threshold event.")
						.defineInRange("residentNutritionSevereReserve",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_SEVERE_RESERVE, 0d, 100d);
					residentNutritionRecoveryDirectWeight = builder
						.comment("Weight of carbohydrate/vegetable alone in mental/health recovery nutrition supply.")
						.defineInRange("residentNutritionRecoveryDirectWeight",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_RECOVERY_DIRECT_WEIGHT, 0d, 100d);
					residentNutritionRecoverySupportWeight = builder
						.comment("Weight of fat/protein-supported carbohydrate/vegetable recovery supply.")
						.defineInRange("residentNutritionRecoverySupportWeight",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_RECOVERY_SUPPORT_WEIGHT, 0d, 100d);
					residentNutritionDeficiencyGrowthFloor = builder
						.comment("Strength/intelligence growth multiplier at zero relevant nutrition reserve.")
						.defineInRange("residentNutritionDeficiencyGrowthFloor",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_DEFICIENCY_GROWTH_FLOOR, 0d, 1d);
					residentNutritionMaximumGrowthBonus = builder
						.comment("Maximum strength/intelligence growth bonus above the healthy reserve.")
						.defineInRange("residentNutritionMaximumGrowthBonus",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_MAXIMUM_GROWTH_BONUS, 0d, 10d);
					residentNutritionMealSelectionChunks = builder
						.comment("Number of decisions used to compose each resident meal; more chunks improve dietary targeting.")
						.defineInRange("residentNutritionMealSelectionChunks",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_MEAL_SELECTION_CHUNKS, 1, 128);
					residentNutritionChannelNeedUtilityWeight = builder
						.comment("Meal-selection weight for replenishing a deficient nutrition channel.")
						.defineInRange("residentNutritionChannelNeedUtilityWeight",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_CHANNEL_NEED_UTILITY_WEIGHT, 0d, 100d);
					residentNutritionConditionNeedUtilityWeight = builder
						.comment("Meal-selection weight for carbohydrate mental need and vegetable health need.")
						.defineInRange("residentNutritionConditionNeedUtilityWeight",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_CONDITION_NEED_UTILITY_WEIGHT, 0d, 100d);
					residentNutritionGrowthNeedUtilityWeight = builder
						.comment("Extra meal-selection weight for fat intelligence growth and child protein strength growth.")
						.defineInRange("residentNutritionGrowthNeedUtilityWeight",
							TownModelParameters.Defaults.RESIDENT_NUTRITION_GROWTH_NEED_UTILITY_WEIGHT, 0d, 100d);
					foodDeficitPenaltyExponent = builder
						.comment("Exponent applied to the missing-food fraction before health and mental penalties.")
						.defineInRange("foodDeficitPenaltyExponent",
							TownModelParameters.Defaults.HOUSING_FOOD_DEFICIT_PENALTY_EXPONENT,
							0.01d, 100d);
					healthLossAtZeroFoodPerResidentDay = builder
						.comment("Health points lost per resident-day when no required food is consumed.")
						.defineInRange("healthLossAtZeroFoodPerResidentDay",
							TownModelParameters.Defaults.HOUSING_HEALTH_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY,
							0d, 100d);
					mentalLossAtZeroFoodPerResidentDay = builder
						.comment("Mental points lost per resident-day when no required food is consumed.")
						.defineInRange("mentalLossAtZeroFoodPerResidentDay",
							TownModelParameters.Defaults.HOUSING_MENTAL_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY,
							0d, 100d);
					maximumHealthRecoveryPerResidentDay = builder
						.comment("Maximum health points recovered per resident-day at health 0 under perfect conditions.")
						.defineInRange("maximumHealthRecoveryPerResidentDay",
							TownModelParameters.Defaults.HOUSING_MAXIMUM_HEALTH_RECOVERY_PER_RESIDENT_DAY,
							0d, 100d);
					maximumMentalRecoveryPerResidentDay = builder
						.comment("Maximum mental points recovered per resident-day at mental 0 under perfect conditions.")
						.defineInRange("maximumMentalRecoveryPerResidentDay",
							TownModelParameters.Defaults.HOUSING_MAXIMUM_MENTAL_RECOVERY_PER_RESIDENT_DAY,
							0d, 100d);

					builder.push("Structure and Capacity");
					minimumFloorAreaBlocks = builder
						.comment("Minimum valid house floor area, in square blocks.")
						.defineInRange("minimumFloorAreaBlocks",
							TownModelParameters.Defaults.HOUSING_MINIMUM_FLOOR_AREA_BLOCKS,
							0, 1000000);
					minimumInteriorVolumeBlocks = builder
						.comment("Minimum valid house interior volume, in cubic blocks.")
						.defineInRange("minimumInteriorVolumeBlocks",
							TownModelParameters.Defaults.HOUSING_MINIMUM_INTERIOR_VOLUME_BLOCKS,
							0, 1000000);
					minimumTemperatureCelsius = builder
						.comment("Minimum effective temperature for assigning residents and avoiding direct cold stress.")
						.defineInRange("minimumTemperatureCelsius",
							TownModelParameters.Defaults.HOUSING_MINIMUM_TEMPERATURE_CELSIUS,
							-1000d, 1000d);
					maximumTemperatureCelsius = builder
						.comment("Maximum effective temperature for assigning residents and avoiding direct heat stress.")
						.defineInRange("maximumTemperatureCelsius",
							TownModelParameters.Defaults.HOUSING_MAXIMUM_TEMPERATURE_CELSIUS,
							-1000d, 1000d);
					floorBlocksPerResident = builder
						.comment("Effective floor blocks required per resident before bed count is applied.")
						.defineInRange("floorBlocksPerResident",
							TownModelParameters.Defaults.HOUSING_FLOOR_BLOCKS_PER_RESIDENT,
							0.01d, 1000000d);
					builder.pop();

					builder.push("Temperature Stress");
					temperatureFullStressDistanceCelsius = builder
						.comment("Degrees outside the safe temperature range at which direct temperature stress reaches 100%.")
						.defineInRange("fullStressDistanceCelsius",
							TownModelParameters.Defaults.HOUSING_TEMPERATURE_FULL_STRESS_DISTANCE_CELSIUS,
							0.01d, 1000d);
					temperatureStressPenaltyExponent = builder
						.comment("Exponent applied to normalized distance outside the safe temperature range.")
						.defineInRange("penaltyExponent",
							TownModelParameters.Defaults.HOUSING_TEMPERATURE_STRESS_PENALTY_EXPONENT,
							0.01d, 100d);
					healthLossAtFullTemperatureStressPerResidentDay = builder
						.comment("Maximum direct health loss per resident-day from cold or heat stress.")
						.defineInRange("healthLossAtFullStressPerResidentDay",
							TownModelParameters.Defaults.HOUSING_HEALTH_LOSS_AT_FULL_TEMPERATURE_STRESS_PER_RESIDENT_DAY,
							0d, 100d);
					mentalLossAtFullTemperatureStressPerResidentDay = builder
						.comment("Maximum direct mental loss per resident-day from cold or heat stress.")
						.defineInRange("mentalLossAtFullStressPerResidentDay",
							TownModelParameters.Defaults.HOUSING_MENTAL_LOSS_AT_FULL_TEMPERATURE_STRESS_PER_RESIDENT_DAY,
							0d, 100d);
					builder.pop();

					builder.push("Comfort Weights");
					temperatureComfortWeight = builder
						.comment("Relative weight of effective-temperature comfort in the unified house rating.")
						.defineInRange("temperatureComfortWeight",
							TownModelParameters.Defaults.HOUSING_TEMPERATURE_COMFORT_WEIGHT,
							0d, 1000d);
					spaceComfortWeight = builder
						.comment("Relative weight of space quality in the unified house rating.")
						.defineInRange("spaceComfortWeight",
							TownModelParameters.Defaults.HOUSING_SPACE_COMFORT_WEIGHT,
							0d, 1000d);
					decorationComfortWeight = builder
						.comment("Relative weight of decoration quality in the unified house rating.")
						.comment("The three comfort weights are normalized by their sum.")
						.defineInRange("decorationComfortWeight",
							TownModelParameters.Defaults.HOUSING_DECORATION_COMFORT_WEIGHT,
							0d, 1000d);
					builder.pop();

					builder.push("Decoration Rating");
					decorationCountLogOffset = builder
						.comment("Positive count offset inside the logarithmic score for each decoration type.")
						.defineInRange("countLogOffset",
							TownModelParameters.Defaults.DECORATION_COUNT_LOG_OFFSET,
							0.000001d, 1000d);
					decorationCountLogMultiplier = builder
						.comment("Multiplier applied to each decoration type's logarithmic count score.")
						.defineInRange("countLogMultiplier",
							TownModelParameters.Defaults.DECORATION_COUNT_LOG_MULTIPLIER,
							0d, 1000d);
					decorationTypeBaseScore = builder
						.comment("Base score added for each decoration type present in the house.")
						.defineInRange("typeBaseScore",
							TownModelParameters.Defaults.DECORATION_TYPE_BASE_SCORE,
							-1000d, 1000d);
					decorationBaseDemand = builder
						.comment("Base decoration score required for a rating of one before floor-area demand.")
						.defineInRange("baseDemand",
							TownModelParameters.Defaults.DECORATION_BASE_DEMAND,
							0.000001d, 1000000d);
					decorationFloorBlocksPerDemand = builder
						.comment("Floor blocks that add one point of decoration demand.")
						.defineInRange("floorBlocksPerDemand",
							TownModelParameters.Defaults.DECORATION_FLOOR_BLOCKS_PER_DEMAND,
							0.000001d, 1000000d);
					builder.pop();
					builder.pop();
				}
			}

			public static class BuildingScoring {
				public final ForgeConfigSpec.DoubleValue comfortableTemperatureCelsius;
				public final ForgeConfigSpec.DoubleValue minimumTemperatureRating;
				public final ForgeConfigSpec.DoubleValue temperatureRatingSlope;
				public final ForgeConfigSpec.DoubleValue temperatureRatingHalfPointDifferenceCelsius;
				public final ForgeConfigSpec.DoubleValue spaceAreaCoefficient;
				public final ForgeConfigSpec.DoubleValue spaceHeightLogCoefficient;
				public final ForgeConfigSpec.DoubleValue spaceHeightLogOffset;
				public final ForgeConfigSpec.DoubleValue spaceResponseScale;
				public final ForgeConfigSpec.DoubleValue spaceResponseExponent;

				BuildingScoring(ForgeConfigSpec.Builder builder) {
					builder.push("Building Scoring");
					comfortableTemperatureCelsius = builder
						.comment("Comfortable indoor temperature used by house and work-building ratings.")
						.defineInRange("comfortableTemperatureCelsius",
							TownModelParameters.Defaults.BUILDING_COMFORTABLE_TEMPERATURE_CELSIUS,
							-1000d, 1000d);
					minimumTemperatureRating = builder
						.comment("Constant floor added to the indoor-temperature sigmoid rating.")
						.defineInRange("minimumTemperatureRating",
							TownModelParameters.Defaults.BUILDING_MINIMUM_TEMPERATURE_RATING,
							0d, 1d);
					temperatureRatingSlope = builder
						.comment("Sigmoid slope per Celsius of distance from the comfortable temperature.")
						.defineInRange("temperatureRatingSlope",
							TownModelParameters.Defaults.BUILDING_TEMPERATURE_RATING_SLOPE,
							0d, 1000d);
					temperatureRatingHalfPointDifferenceCelsius = builder
						.comment("Temperature difference from comfort at the sigmoid's one-half point.")
						.defineInRange("temperatureRatingHalfPointDifferenceCelsius",
							TownModelParameters.Defaults.BUILDING_TEMPERATURE_RATING_HALF_POINT_DIFFERENCE_CELSIUS,
							0d, 1000d);
					spaceAreaCoefficient = builder
						.comment("Base effective-space score contributed by each floor block.")
						.defineInRange("spaceAreaCoefficient",
							TownModelParameters.Defaults.BUILDING_SPACE_AREA_COEFFICIENT,
							-1000d, 1000d);
					spaceHeightLogCoefficient = builder
						.comment("Multiplier of the logarithmic average-height term in space scoring.")
						.defineInRange("spaceHeightLogCoefficient",
							TownModelParameters.Defaults.BUILDING_SPACE_HEIGHT_LOG_COEFFICIENT,
							-1000d, 1000d);
					spaceHeightLogOffset = builder
						.comment("Average-height offset subtracted before taking the space-score logarithm.")
						.defineInRange("spaceHeightLogOffset",
							TownModelParameters.Defaults.BUILDING_SPACE_HEIGHT_LOG_OFFSET,
							-1000d, 1000d);
					spaceResponseScale = builder
						.comment("Scale of the exponential response converting effective space to a 0-1 rating.")
						.defineInRange("spaceResponseScale",
							TownModelParameters.Defaults.BUILDING_SPACE_RESPONSE_SCALE,
							0d, 1000d);
					spaceResponseExponent = builder
						.comment("Exponent applied to effective space before the exponential response.")
						.defineInRange("spaceResponseExponent",
							TownModelParameters.Defaults.BUILDING_SPACE_RESPONSE_EXPONENT,
							0d, 1000d);
					builder.pop();
				}
			}

			public static class ResidentProgression {
				public final ForgeConfigSpec.DoubleValue maximumWorkProficiency;
				public final ForgeConfigSpec.DoubleValue proficiencyGrowthAtZeroPerWorkday;
				public final ForgeConfigSpec.DoubleValue minimumProficiencyGrowthPerWorkday;

				ResidentProgression(ForgeConfigSpec.Builder builder) {
					builder.push("Resident Progression");
					maximumWorkProficiency = builder
						.comment("Maximum stored profession proficiency for every resident.")
						.defineInRange("maximumWorkProficiency",
							TownModelParameters.Defaults.RESIDENT_MAXIMUM_WORK_PROFICIENCY,
							1d, 1000000d);
					proficiencyGrowthAtZeroPerWorkday = builder
						.comment("Profession proficiency gained per effective workday at proficiency 0.")
						.comment("Growth decreases linearly as proficiency approaches 100.")
						.defineInRange("proficiencyGrowthAtZeroPerWorkday",
							TownModelParameters.Defaults.RESIDENT_PROFICIENCY_GROWTH_AT_ZERO_PER_WORKDAY,
							0d, 100d);
					minimumProficiencyGrowthPerWorkday = builder
						.comment("Minimum profession proficiency gained per effective workday below proficiency 100.")
						.defineInRange("minimumProficiencyGrowthPerWorkday",
							TownModelParameters.Defaults.RESIDENT_MINIMUM_PROFICIENCY_GROWTH_PER_WORKDAY,
							0d, 100d);
					builder.pop();
				}
			}

			public static class ResidentRules {
				public final ForgeConfigSpec.DoubleValue homelessHealthLossPerDay;
				public final ForgeConfigSpec.DoubleValue removalHealthThreshold;
				public final ForgeConfigSpec.DoubleValue removalMentalThreshold;
				public final ForgeConfigSpec.IntValue minimumWorkingAge;
				public final ForgeConfigSpec.DoubleValue minimumWorkingHealthExclusive;
				public final ForgeConfigSpec.DoubleValue minimumWorkingMentalExclusive;
				public final ForgeConfigSpec.BooleanValue workRequiresHousing;
				public final ForgeConfigSpec.DoubleValue residentialCareScoreBand;
				public final ForgeConfigSpec.IntValue townPolicyCooldownDays;

				ResidentRules(ForgeConfigSpec.Builder builder) {
					builder.push("Resident Rules");
					homelessHealthLossPerDay = builder
						.comment("Health lost each morning by a resident without an assigned house.")
						.defineInRange("homelessHealthLossPerDay",
							TownModelParameters.Defaults.RESIDENT_HOMELESS_HEALTH_LOSS_PER_DAY,
							0d, 100d);
					removalHealthThreshold = builder
						.comment("Residents at or below this health value are removed during morning settlement.")
						.defineInRange("removalHealthThreshold",
							TownModelParameters.Defaults.RESIDENT_REMOVAL_HEALTH_THRESHOLD,
							0d, 100d);
					removalMentalThreshold = builder
						.comment("Residents at or below this mental value leave during morning settlement.")
						.defineInRange("removalMentalThreshold",
							TownModelParameters.Defaults.RESIDENT_REMOVAL_MENTAL_THRESHOLD,
							0d, 100d);
					minimumWorkingAge = builder
						.comment("Minimum resident age group allowed to work: 0 infant, 1 child, 2 adult, 3 elder.")
						.defineInRange("minimumWorkingAge",
							TownModelParameters.Defaults.RESIDENT_MINIMUM_WORKING_AGE,
							0, 3);
					minimumWorkingHealthExclusive = builder
						.comment("A resident must have health strictly greater than this value to work.")
						.defineInRange("minimumWorkingHealthExclusive",
							TownModelParameters.Defaults.RESIDENT_MINIMUM_WORKING_HEALTH_EXCLUSIVE,
							0d, 100d);
					minimumWorkingMentalExclusive = builder
						.comment("A resident must have mental strictly greater than this value to work.")
						.defineInRange("minimumWorkingMentalExclusive",
							TownModelParameters.Defaults.RESIDENT_MINIMUM_WORKING_MENTAL_EXCLUSIVE,
							0d, 100d);
					workRequiresHousing = builder
						.comment("Require an assigned house before a resident can work.")
						.define("workRequiresHousing",
							TownModelParameters.Defaults.RESIDENT_WORK_REQUIRES_HOUSING);
					residentialCareScoreBand = builder
						.comment("Risk-score band within which existing house residency may break residential-care ties.")
						.defineInRange("residentialCareScoreBand",
							TownModelParameters.Defaults.RESIDENTIAL_CARE_SCORE_BAND, 0.000001d, 1d);
					townPolicyCooldownDays = builder
						.comment("Town days before another mayoral policy change may be requested.")
						.defineInRange("townPolicyCooldownDays",
							TownModelParameters.Defaults.TOWN_POLICY_COOLDOWN_DAYS, 0, 3650);
					builder.pop();
				}
			}

			public static class ResidentGeneration {
				public final ForgeConfigSpec.DoubleValue initialHealth;
				public final ForgeConfigSpec.DoubleValue initialMental;
				public final ForgeConfigSpec.IntValue attributeSampleCount;
				public final ForgeConfigSpec.DoubleValue infantStrengthCenter;
				public final ForgeConfigSpec.DoubleValue infantIntelligenceCenter;
				public final ForgeConfigSpec.DoubleValue childStrengthCenter;
				public final ForgeConfigSpec.DoubleValue childIntelligenceCenter;
				public final ForgeConfigSpec.DoubleValue adultStrengthCenter;
				public final ForgeConfigSpec.DoubleValue adultIntelligenceCenter;
				public final ForgeConfigSpec.DoubleValue elderStrengthCenter;
				public final ForgeConfigSpec.DoubleValue elderIntelligenceCenter;
				public final ForgeConfigSpec.DoubleValue nonAdultAttributeSpread;
				public final ForgeConfigSpec.DoubleValue adultAttributeSpread;
				public final ForgeConfigSpec.DoubleValue infantInitialProficiency;
				public final ForgeConfigSpec.DoubleValue childMaximumInitialProficiency;
				public final ForgeConfigSpec.DoubleValue adultMaximumInitialProficiency;
				public final ForgeConfigSpec.DoubleValue elderMinimumInitialProficiency;
				public final ForgeConfigSpec.DoubleValue elderMaximumInitialProficiency;
				public final ForgeConfigSpec.IntValue adultAgeRangeDaysExclusive;
				public final ForgeConfigSpec.DoubleValue fallbackWeightInfant;
				public final ForgeConfigSpec.DoubleValue fallbackWeightChild;
				public final ForgeConfigSpec.DoubleValue fallbackWeightAdult;
				public final ForgeConfigSpec.DoubleValue fallbackWeightElder;
				public final ForgeConfigSpec.DoubleValue coldSurvivorHealthMinimum;
				public final ForgeConfigSpec.DoubleValue coldSurvivorHealthMaximum;
				public final ForgeConfigSpec.DoubleValue coldSurvivorAttributeBonus;
				public final ForgeConfigSpec.DoubleValue coldSurvivorProficiencyMultiplier;

				ResidentGeneration(ForgeConfigSpec.Builder builder) {
					builder.push("Resident Generation");
					initialHealth = builder.comment("Health assigned to an ordinary newly recruited resident.")
						.defineInRange("initialHealth", TownModelParameters.Defaults.RESIDENT_INITIAL_HEALTH, 0d, 100d);
					initialMental = builder.comment("Mental state assigned to an ordinary newly recruited resident.")
						.defineInRange("initialMental", TownModelParameters.Defaults.RESIDENT_INITIAL_MENTAL, 0d, 100d);
					attributeSampleCount = builder.comment("Uniform samples averaged for each initial strength/intelligence draw; larger values concentrate residents near the age-group center.")
						.defineInRange("attributeSampleCount", TownModelParameters.Defaults.RESIDENT_ATTRIBUTE_SAMPLE_COUNT, 1, 100);
					infantStrengthCenter = attribute(builder, "infantStrengthCenter", "Infant initial strength distribution center.", TownModelParameters.Defaults.RESIDENT_INFANT_STRENGTH_CENTER);
					infantIntelligenceCenter = attribute(builder, "infantIntelligenceCenter", "Infant initial intelligence distribution center.", TownModelParameters.Defaults.RESIDENT_INFANT_INTELLIGENCE_CENTER);
					childStrengthCenter = attribute(builder, "childStrengthCenter", "Child initial strength distribution center.", TownModelParameters.Defaults.RESIDENT_CHILD_STRENGTH_CENTER);
					childIntelligenceCenter = attribute(builder, "childIntelligenceCenter", "Child initial intelligence distribution center.", TownModelParameters.Defaults.RESIDENT_CHILD_INTELLIGENCE_CENTER);
					adultStrengthCenter = attribute(builder, "adultStrengthCenter", "Adult initial strength distribution center.", TownModelParameters.Defaults.RESIDENT_ADULT_STRENGTH_CENTER);
					adultIntelligenceCenter = attribute(builder, "adultIntelligenceCenter", "Adult initial intelligence distribution center.", TownModelParameters.Defaults.RESIDENT_ADULT_INTELLIGENCE_CENTER);
					elderStrengthCenter = attribute(builder, "elderStrengthCenter", "Elder initial strength distribution center.", TownModelParameters.Defaults.RESIDENT_ELDER_STRENGTH_CENTER);
					elderIntelligenceCenter = attribute(builder, "elderIntelligenceCenter", "Elder initial intelligence distribution center.", TownModelParameters.Defaults.RESIDENT_ELDER_INTELLIGENCE_CENTER);
					nonAdultAttributeSpread = builder.comment("Width multiplier of infant, child, and elder initial attribute distributions.")
						.defineInRange("nonAdultAttributeSpread", TownModelParameters.Defaults.RESIDENT_NON_ADULT_ATTRIBUTE_SPREAD, 0d, 2d);
					adultAttributeSpread = builder.comment("Width multiplier of adult initial attribute distributions.")
						.defineInRange("adultAttributeSpread", TownModelParameters.Defaults.RESIDENT_ADULT_ATTRIBUTE_SPREAD, 0d, 2d);
					infantInitialProficiency = proficiency(builder, "infantInitialProficiency", "Initial proficiency assigned to infants.", TownModelParameters.Defaults.RESIDENT_INFANT_INITIAL_PROFICIENCY);
					childMaximumInitialProficiency = proficiency(builder, "childMaximumInitialProficiency", "Upper bound before the squared low-skill bias for child initial proficiency.", TownModelParameters.Defaults.RESIDENT_CHILD_MAXIMUM_INITIAL_PROFICIENCY);
					adultMaximumInitialProficiency = proficiency(builder, "adultMaximumInitialProficiency", "Upper bound before the squared low-skill bias for adult initial proficiency.", TownModelParameters.Defaults.RESIDENT_ADULT_MAXIMUM_INITIAL_PROFICIENCY);
					elderMinimumInitialProficiency = proficiency(builder, "elderMinimumInitialProficiency", "Lower bound of the uniform elder initial proficiency distribution.", TownModelParameters.Defaults.RESIDENT_ELDER_MINIMUM_INITIAL_PROFICIENCY);
					elderMaximumInitialProficiency = proficiency(builder, "elderMaximumInitialProficiency", "Upper bound of the uniform elder initial proficiency distribution.", TownModelParameters.Defaults.RESIDENT_ELDER_MAXIMUM_INITIAL_PROFICIENCY);
					adultAgeRangeDaysExclusive = builder.comment("Random age-day span added after childToAdultDays for directly recruited adults and elders.")
						.defineInRange("adultAgeRangeDaysExclusive", TownModelParameters.Defaults.RESIDENT_ADULT_AGE_RANGE_DAYS_EXCLUSIVE, 1, 1000000);
					fallbackWeightInfant = weight(builder, "fallbackWeightInfant", TownModelParameters.Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_INFANT);
					fallbackWeightChild = weight(builder, "fallbackWeightChild", TownModelParameters.Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_CHILD);
					fallbackWeightAdult = weight(builder, "fallbackWeightAdult", TownModelParameters.Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_ADULT);
					fallbackWeightElder = weight(builder, "fallbackWeightElder", TownModelParameters.Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_ELDER);
					coldSurvivorHealthMinimum = attribute(builder, "coldSurvivorHealthMinimum", "Minimum health of a cold-current high-quality survivor.", TownModelParameters.Defaults.RESIDENT_COLD_SURVIVOR_HEALTH_MINIMUM);
					coldSurvivorHealthMaximum = attribute(builder, "coldSurvivorHealthMaximum", "Maximum health of a cold-current high-quality survivor.", TownModelParameters.Defaults.RESIDENT_COLD_SURVIVOR_HEALTH_MAXIMUM);
					coldSurvivorAttributeBonus = attribute(builder, "coldSurvivorAttributeBonus", "Strength and intelligence bonus of a cold-current high-quality survivor.", TownModelParameters.Defaults.RESIDENT_COLD_SURVIVOR_ATTRIBUTE_BONUS);
					coldSurvivorProficiencyMultiplier = builder.comment("Work-proficiency multiplier of a cold-current high-quality survivor.")
						.defineInRange("coldSurvivorProficiencyMultiplier", TownModelParameters.Defaults.RESIDENT_COLD_SURVIVOR_PROFICIENCY_MULTIPLIER, 0d, 100d);
					builder.pop();
				}

				private static ForgeConfigSpec.DoubleValue attribute(ForgeConfigSpec.Builder builder, String key, String comment, double value) {
					return builder.comment(comment).defineInRange(key, value, 0d, 100d);
				}

				private static ForgeConfigSpec.DoubleValue proficiency(ForgeConfigSpec.Builder builder, String key, String comment, double value) {
					return builder.comment(comment).defineInRange(key, value, 0d, 100d);
				}

				private static ForgeConfigSpec.DoubleValue weight(ForgeConfigSpec.Builder builder, String key, double value) {
					return builder.comment("Fallback age weight used only when all configured refugee age weights are zero.")
						.defineInRange(key, value, 0d, 1000d);
				}
			}

			public static class RefugeeSpawn {
				public final ForgeConfigSpec.BooleanValue enableRefugeeSpawn;
				public final ForgeConfigSpec.DoubleValue baseSpawnChancePerDay;
				public final ForgeConfigSpec.DoubleValue warmSpawnChanceBonus;
				public final ForgeConfigSpec.IntValue warmSpawnBatchBonus;
				public final ForgeConfigSpec.DoubleValue coldSpawnChancePenalty;
				public final ForgeConfigSpec.IntValue coldSpawnBatchPenalty;
				public final ForgeConfigSpec.DoubleValue coldQualityChance;
				public final ForgeConfigSpec.IntValue spawnRadiusMinBlocks;
				public final ForgeConfigSpec.IntValue spawnRadiusMaxBlocks;
				public final ForgeConfigSpec.IntValue batchSizeMin;
				public final ForgeConfigSpec.IntValue batchSizeMax;
				public final ForgeConfigSpec.DoubleValue weightInfant;
				public final ForgeConfigSpec.DoubleValue weightChild;
				public final ForgeConfigSpec.DoubleValue weightAdult;
				public final ForgeConfigSpec.DoubleValue weightElder;
				public final ForgeConfigSpec.IntValue maxWaitDays;

				RefugeeSpawn(ForgeConfigSpec.Builder builder) {
					builder.push("Refugee Spawn");
					enableRefugeeSpawn = builder.comment("Enables the daily wandering-refugee batch near the team's energy tower (能量塔),")
						.comment("gated on the tower being switched on and the daily weather roll.")
						.define("enableRefugeeSpawn", true);
					baseSpawnChancePerDay = builder.comment("Base chance of a refugee batch spawning on any morning.")
						.comment("Modified by warm/cold weather, clamped to [0,1].")
						.defineInRange("baseSpawnChancePerDay", 0.6d, 0d, 1d);
					warmSpawnChanceBonus = builder.comment("Spawn chance bonus during a warm current (温度级别>=1) with sunny weather.")
						.defineInRange("warmSpawnChanceBonus", 0.3d, 0d, 1d);
					warmSpawnBatchBonus = builder.comment("Extra refugees per batch during a warm sunny day.")
						.defineInRange("warmSpawnBatchBonus", 1, 0, 10);
					coldSpawnChancePenalty = builder.comment("Spawn chance penalty during a cold current (温度级别<=-1) or a blizzard.")
						.defineInRange("coldSpawnChancePenalty", 0.3d, 0d, 1d);
					coldSpawnBatchPenalty = builder.comment("Fewer refugees per batch during a cold current or blizzard.")
						.defineInRange("coldSpawnBatchPenalty", 1, 0, 10);
					coldQualityChance = builder.comment("Chance that a refugee spawned during a cold current is high-quality but low-health")
						.comment("The health range, attribute bonus, and proficiency multiplier are configured under Resident Generation.")
						.defineInRange("coldQualityChance", TownModelParameters.Defaults.RESIDENT_COLD_SURVIVOR_CHANCE, 0d, 1d);
					spawnRadiusMinBlocks = builder.comment("Minimum horizontal distance from the tower master block for refugee spawns, in blocks.")
						.defineInRange("spawnRadiusMinBlocks", 8, 0, 64);
					spawnRadiusMaxBlocks = builder.comment("Maximum horizontal distance from the tower master block for refugee spawns, in blocks.")
						.defineInRange("spawnRadiusMaxBlocks", 24, 0, 128);
					batchSizeMin = builder.comment("Minimum refugees spawned per daily batch (before weather modifiers).")
						.defineInRange("batchSizeMin", 1, 0, 10);
					batchSizeMax = builder.comment("Maximum refugees spawned per daily batch (before weather modifiers).")
						.defineInRange("batchSizeMax", 3, 1, 10);
					weightInfant = builder.comment("Relative weight of infants (age 0) in each batch.")
						.defineInRange("weightInfant", TownModelParameters.Defaults.RESIDENT_AGE_WEIGHT_INFANT, 0d, 1000d);
					weightChild = builder.comment("Relative weight of children (age 1) in each batch.")
						.defineInRange("weightChild", TownModelParameters.Defaults.RESIDENT_AGE_WEIGHT_CHILD, 0d, 1000d);
					weightAdult = builder.comment("Relative weight of young adults (age 2) in each batch.")
						.defineInRange("weightAdult", TownModelParameters.Defaults.RESIDENT_AGE_WEIGHT_ADULT, 0d, 1000d);
					weightElder = builder.comment("Relative weight of elders (age 3) in each batch.")
						.defineInRange("weightElder", TownModelParameters.Defaults.RESIDENT_AGE_WEIGHT_ELDER, 0d, 1000d);
					maxWaitDays = builder.comment("Days a town-spawned refugee waits near the tower before leaving on their own.")
						.comment("They also leave the first morning the town has no vacant house.")
						.defineInRange("maxWaitDays", 3, 1, 100);
					builder.pop();
				}
			}

			public static class ResidentAging {
				public final ForgeConfigSpec.IntValue infantToChildDays;
				public final ForgeConfigSpec.IntValue childToAdultDays;
				public final ForgeConfigSpec.DoubleValue infantStrengthGainPerDay;
				public final ForgeConfigSpec.DoubleValue infantIntelligenceGainPerDay;
				public final ForgeConfigSpec.DoubleValue infantAttributeCap;
				public final ForgeConfigSpec.DoubleValue childStrengthGainPerDay;
				public final ForgeConfigSpec.DoubleValue childIntelligenceGainPerDay;
				public final ForgeConfigSpec.DoubleValue childStrengthCap;
				public final ForgeConfigSpec.DoubleValue childIntelligenceCap;
				public final ForgeConfigSpec.DoubleValue adultStrengthGainPerDay;
				public final ForgeConfigSpec.DoubleValue adultIntelligenceGainPerDay;
				public final ForgeConfigSpec.DoubleValue adultAttributeCap;
				public final ForgeConfigSpec.DoubleValue elderStrengthDecayPerDay;
				public final ForgeConfigSpec.DoubleValue elderStrengthFloor;

				ResidentAging(ForgeConfigSpec.Builder builder) {
					builder.push("Resident Aging");
					infantToChildDays = builder.comment("Age-days at which an infant (0) grows into a child (1).")
						.defineInRange("infantToChildDays", TownModelParameters.Defaults.RESIDENT_INFANT_TO_CHILD_DAYS, 1, 100);
					childToAdultDays = builder.comment("Age-days at which a child (1) grows into a young adult (2).")
						.comment("Elders (3) never grow in this way; they only spawn naturally.")
						.defineInRange("childToAdultDays", TownModelParameters.Defaults.RESIDENT_CHILD_TO_ADULT_DAYS, 1, 100);
					infantStrengthGainPerDay = builder.comment("Strength gained per day by infants (age 0).")
						.defineInRange("infantStrengthGainPerDay", TownModelParameters.Defaults.RESIDENT_INFANT_STRENGTH_GAIN_PER_DAY, 0d, 100d);
					infantIntelligenceGainPerDay = builder.comment("Intelligence gained per day by infants (age 0).")
						.defineInRange("infantIntelligenceGainPerDay", TownModelParameters.Defaults.RESIDENT_INFANT_INTELLIGENCE_GAIN_PER_DAY, 0d, 100d);
					infantAttributeCap = builder.comment("Strength/intelligence cap for infants (age 0).")
						.defineInRange("infantAttributeCap", TownModelParameters.Defaults.RESIDENT_INFANT_ATTRIBUTE_CAP, 0d, 100d);
					childStrengthGainPerDay = builder.comment("Strength gained per day by children (age 1).")
						.defineInRange("childStrengthGainPerDay", TownModelParameters.Defaults.RESIDENT_CHILD_STRENGTH_GAIN_PER_DAY, 0d, 100d);
					childIntelligenceGainPerDay = builder.comment("Intelligence gained per day by children (age 1).")
						.defineInRange("childIntelligenceGainPerDay", TownModelParameters.Defaults.RESIDENT_CHILD_INTELLIGENCE_GAIN_PER_DAY, 0d, 100d);
					childStrengthCap = builder.comment("Strength cap for children (age 1). Higher than the adult starting average so grown children can outstrip direct recruits.")
						.defineInRange("childStrengthCap", TownModelParameters.Defaults.RESIDENT_CHILD_STRENGTH_CAP, 0d, 100d);
					childIntelligenceCap = builder.comment("Intelligence cap for children (age 1).")
						.defineInRange("childIntelligenceCap", TownModelParameters.Defaults.RESIDENT_CHILD_INTELLIGENCE_CAP, 0d, 100d);
					adultStrengthGainPerDay = builder.comment("Strength gained per day by young adults (age 2). Very slow.")
						.defineInRange("adultStrengthGainPerDay", TownModelParameters.Defaults.RESIDENT_ADULT_STRENGTH_GAIN_PER_DAY, 0d, 100d);
					adultIntelligenceGainPerDay = builder.comment("Intelligence gained per day by young adults (age 2). Very slow.")
						.defineInRange("adultIntelligenceGainPerDay", TownModelParameters.Defaults.RESIDENT_ADULT_INTELLIGENCE_GAIN_PER_DAY, 0d, 100d);
					adultAttributeCap = builder.comment("Strength/intelligence cap for young adults (age 2).")
						.defineInRange("adultAttributeCap", TownModelParameters.Defaults.RESIDENT_ADULT_ATTRIBUTE_CAP, 0d, 100d);
					elderStrengthDecayPerDay = builder.comment("Strength lost per day by elders (age 3); decay never drops below the floor.")
						.defineInRange("elderStrengthDecayPerDay", TownModelParameters.Defaults.RESIDENT_ELDER_STRENGTH_DECAY_PER_DAY, 0d, 100d);
					elderStrengthFloor = builder.comment("Strength floor for elders (age 3).")
						.defineInRange("elderStrengthFloor", TownModelParameters.Defaults.RESIDENT_ELDER_STRENGTH_FLOOR, 0d, 100d);
					builder.pop();
				}
			}

			public static class Hunting {
				/**
				 * Hunting production is settled once per town day by tickMorning().
				 * A standard worker has health, mental, strength and intelligence all
				 * equal to 50, and zero hunting proficiency.
				 */
				public final ForgeConfigSpec.DoubleValue expectedLootRollsPerStandardWorkerDay;
				public final ForgeConfigSpec.DoubleValue passiveExpectedLootRollsPerBaseDay;
				public final ForgeConfigSpec.BooleanValue useFractionalLootRollCarry;
				public final ForgeConfigSpec.DoubleValue floorBlocksPerWorkerSlot;
				public final ForgeConfigSpec.IntValue minimumWorkerSlots;
				public final ForgeConfigSpec.IntValue minimumFloorAreaBlocks;
				public final ForgeConfigSpec.IntValue minimumInteriorVolumeBlocks;
				public final ForgeConfigSpec.DoubleValue minimumWorkingTemperatureCelsius;
				public final ForgeConfigSpec.DoubleValue productivityAtAttributeZero;
				public final ForgeConfigSpec.DoubleValue productivityAtAttributeHundred;
				public final ForgeConfigSpec.DoubleValue maximumProficiency;
				public final ForgeConfigSpec.DoubleValue bonusAtMaximumProficiency;
				public final ForgeConfigSpec.DoubleValue minimumResidentProductivity;
				public final ForgeConfigSpec.DoubleValue maximumResidentProductivity;
				public final ForgeConfigSpec.DoubleValue healthWeight;
				public final ForgeConfigSpec.DoubleValue mentalWeight;
				public final ForgeConfigSpec.DoubleValue strengthWeight;
				public final ForgeConfigSpec.DoubleValue intelligenceWeight;
				public final ForgeConfigSpec.DoubleValue spaceRatingWeight;
				public final ForgeConfigSpec.DoubleValue temperatureRatingWeight;
				public final ForgeConfigSpec.DoubleValue assignmentBasePriority;
				public final ForgeConfigSpec.DoubleValue assignmentPenaltyPerWorker;
				public final ForgeConfigSpec.DoubleValue assignmentFillRatioBonus;
				public final ForgeConfigSpec.DoubleValue assignmentRatingMultiplier;
				public final ForgeConfigSpec.IntValue heatEndpointPriority;
				public final ForgeConfigSpec.DoubleValue heatConsumptionPerTick;
				public final ForgeConfigSpec.DoubleValue heatTemperatureLevelScaleCelsius;
				public final ForgeConfigSpec.DoubleValue minimumHeatingModifierCelsius;

				Hunting(ForgeConfigSpec.Builder builder) {
					builder.push("Hunting");
					expectedLootRollsPerStandardWorkerDay = builder
						.comment("Long-run expected hunting-loot-table rolls per standard worker per Minecraft day.")
						.comment("Fractional rolls are retained by each hunting base when fractional carry is enabled.")
						.comment("A standard worker has all four attributes at 50 and zero hunting proficiency.")
						.comment("The default loot table averages 1.5 item units per roll, so the default 7/6 rolls equal 1.75 expected items.")
						.comment("One executed roll consumes one hunt terrain-resource unit even if it yields no stored item.")
						.defineInRange("expectedLootRollsPerStandardWorkerDay",
							TownModelParameters.Defaults.HUNTING_EXPECTED_LOOT_ROLLS_PER_SWE_DAY,
							0d, 1000000d);
					passiveExpectedLootRollsPerBaseDay = builder
						.comment("Long-run expected loot-table rolls supplied by each workable hunting base without labor.")
						.comment("The default of 0 requires productive workers.")
						.defineInRange("passiveExpectedLootRollsPerBaseDay",
							TownModelParameters.Defaults.HUNTING_PASSIVE_EXPECTED_LOOT_ROLLS_PER_BASE_DAY,
							0d, 1000000d);
					useFractionalLootRollCarry = builder
						.comment("Retain fractional expected rolls on each hunting base for exact long-run settlement.")
						.comment("When disabled, expected rolls are rounded down independently each day.")
						.define("useFractionalLootRollCarry",
							TownModelParameters.Defaults.HUNTING_USE_FRACTIONAL_LOOT_ROLL_CARRY);
					floorBlocksPerWorkerSlot = builder
						.comment("Effective floor area required for one hunting-base worker slot, in blocks per worker.")
						.comment("Space rating multiplies effective floor area before slots are calculated.")
						.defineInRange("floorBlocksPerWorkerSlot",
							TownModelParameters.Defaults.HUNTING_FLOOR_BLOCKS_PER_WORKER_SLOT,
							0.01d, 1000000d);
					minimumWorkerSlots = builder
						.comment("Minimum worker slots granted to every structurally valid hunting base, in workers.")
						.defineInRange("minimumWorkerSlots",
							TownModelParameters.Defaults.HUNTING_MINIMUM_WORKER_SLOTS,
							0, 4096);
					minimumFloorAreaBlocks = builder
						.comment("Minimum valid hunting-base floor area, in square blocks.")
						.defineInRange("minimumFloorAreaBlocks",
							TownModelParameters.Defaults.HUNTING_MINIMUM_FLOOR_AREA_BLOCKS,
							0, 1000000);
					minimumInteriorVolumeBlocks = builder
						.comment("Minimum valid hunting-base interior volume, in cubic blocks.")
						.defineInRange("minimumInteriorVolumeBlocks",
							TownModelParameters.Defaults.HUNTING_MINIMUM_INTERIOR_VOLUME_BLOCKS,
							0, 1000000);
					minimumWorkingTemperatureCelsius = builder
						.comment("Minimum effective indoor temperature at which the hunting base can work, in degrees Celsius.")
						.defineInRange("minimumWorkingTemperatureCelsius",
							TownModelParameters.Defaults.HUNTING_MINIMUM_WORKING_TEMPERATURE_CELSIUS,
							-1000d, 1000d);

					builder.push("Resident Productivity");
					productivityAtAttributeZero = builder
						.comment("Relative hunting productivity at weighted attribute 0 and proficiency 0.")
						.defineInRange("productivityAtAttributeZero",
							TownModelParameters.Defaults.HUNTING_PRODUCTIVITY_AT_ATTRIBUTE_ZERO,
							0d, 100d);
					productivityAtAttributeHundred = builder
						.comment("Relative hunting productivity at weighted attribute 100 and proficiency 0.")
						.comment("Linear interpolation makes weighted attribute 50 equal 1.0 with the defaults.")
						.defineInRange("productivityAtAttributeHundred",
							TownModelParameters.Defaults.HUNTING_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED,
							0d, 100d);
					maximumProficiency = builder
						.comment("Profession proficiency that grants the full configured productivity bonus.")
						.defineInRange("maximumProficiency",
							TownModelParameters.Defaults.HUNTING_MAXIMUM_PROFICIENCY, 1d, 100d);
					bonusAtMaximumProficiency = builder
						.comment("Additive relative productivity granted at maximum hunting proficiency.")
						.defineInRange("bonusAtMaximumProficiency",
							TownModelParameters.Defaults.HUNTING_BONUS_AT_MAXIMUM_PROFICIENCY,
							0d, 100d);
					minimumResidentProductivity = builder
						.comment("Minimum final hunting productivity in standard-worker units.")
						.defineInRange("minimumResidentProductivity",
							TownModelParameters.Defaults.HUNTING_MINIMUM_PRODUCTIVITY, 0d, 100d);
					maximumResidentProductivity = builder
						.comment("Maximum final hunting productivity in standard-worker units.")
						.defineInRange("maximumResidentProductivity",
							TownModelParameters.Defaults.HUNTING_MAXIMUM_PRODUCTIVITY, 0d, 100d);
					builder.pop();

					builder.push("Attribute Weights");
					healthWeight = defineHuntingAttributeWeight(builder, "healthWeight", "health",
						TownModelParameters.Defaults.HUNTING_HEALTH_WEIGHT);
					mentalWeight = defineHuntingAttributeWeight(builder, "mentalWeight", "mental",
						TownModelParameters.Defaults.HUNTING_MENTAL_WEIGHT);
					strengthWeight = defineHuntingAttributeWeight(builder, "strengthWeight", "strength",
						TownModelParameters.Defaults.HUNTING_STRENGTH_WEIGHT);
					intelligenceWeight = defineHuntingAttributeWeight(builder, "intelligenceWeight", "intelligence",
						TownModelParameters.Defaults.HUNTING_INTELLIGENCE_WEIGHT);
					builder.pop();

					builder.push("Building Rating");
					spaceRatingWeight = builder
						.comment("Relative dimensionless weight of space quality in hunting-base rating.")
						.defineInRange("spaceWeight",
							TownModelParameters.Defaults.HUNTING_SPACE_RATING_WEIGHT,
							0d, 1000d);
					temperatureRatingWeight = builder
						.comment("Relative dimensionless weight of temperature quality in hunting-base rating.")
						.comment("Setting both rating weights to zero makes building rating zero.")
						.defineInRange("temperatureWeight",
							TownModelParameters.Defaults.HUNTING_TEMPERATURE_RATING_WEIGHT,
							0d, 1000d);
					builder.pop();

					builder.push("Worker Assignment");
					assignmentBasePriority = builder
						.comment("Deprecated compatibility value; the town-level staffing queue now controls assignment and ignores this setting.")
						.defineInRange("basePriority",
							TownModelParameters.Defaults.HUNTING_ASSIGNMENT_BASE_PRIORITY,
							-1000000d, 1000000d);
					assignmentPenaltyPerWorker = builder
						.comment("Deprecated compatibility value; ignored by the current staffing planner.")
						.defineInRange("penaltyPerWorker",
							TownModelParameters.Defaults.HUNTING_ASSIGNMENT_PENALTY_PER_WORKER,
							0d, 1000000d);
					assignmentFillRatioBonus = builder
						.comment("Deprecated compatibility value; ignored by the current staffing planner.")
						.defineInRange("fillRatioBonus",
							TownModelParameters.Defaults.HUNTING_ASSIGNMENT_FILL_RATIO_BONUS,
							-1000000d, 1000000d);
					assignmentRatingMultiplier = builder
						.comment("Deprecated compatibility value; ignored by the current staffing planner.")
						.defineInRange("ratingMultiplier",
							TownModelParameters.Defaults.HUNTING_ASSIGNMENT_RATING_MULTIPLIER,
							-1000000d, 1000000d);
					builder.pop();

					builder.push("Heating");
					heatEndpointPriority = builder
						.comment("Heat-network consumer priority. Lower-priority endpoints detach first when heat is insufficient.")
						.comment("Changing this for an existing block entity requires its chunk to be reloaded.")
						.defineInRange("endpointPriority", 99, -1000000, 1000000);
					heatConsumptionPerTick = builder
						.comment("Heat consumed by an active hunting base per game tick, in heat units per tick.")
						.comment("Set to 0 to disable hunting-base network heating. Changing it may require a chunk reload.")
						.defineInRange("consumptionPerTick", 1d, 0d, 1000000d);
					heatTemperatureLevelScaleCelsius = builder
						.comment("Degrees Celsius of temperature modifier per heat-network temperature level.")
						.defineInRange("temperatureLevelScaleCelsius", 10d, -1000d, 1000d);
					minimumHeatingModifierCelsius = builder
						.comment("Minimum temperature modifier while heat is successfully consumed, in degrees Celsius.")
						.defineInRange("minimumModifierCelsius", 24d, -1000d, 1000d);
					builder.pop();
					builder.pop();
				}

				private static ForgeConfigSpec.DoubleValue defineHuntingAttributeWeight(
						ForgeConfigSpec.Builder builder, String key, String attributeName, double defaultValue) {
					return builder
						.comment("Relative dimensionless weight of " + attributeName + " in hunting productivity.")
						.comment("Weights are normalized by their sum; setting every weight to zero uses an equal-weight average.")
						.defineInRange(key, defaultValue, 0d, 1000d);
				}
			}
			public static class Mining {
				/**
				 * Mining production is settled once per town day by tickMorning().
				 * A standard worker has health, mental, strength and intelligence all
				 * equal to 50, and zero mining proficiency.
				 */
				public final ForgeConfigSpec.DoubleValue baseOutputPerStandardWorkerDay;
				public final ForgeConfigSpec.DoubleValue floorBlocksPerWorkerSlot;
				public final ForgeConfigSpec.IntValue minimumWorkerSlots;
				public final ForgeConfigSpec.IntValue connectionRadiusBlocks;
				public final ForgeConfigSpec.DoubleValue productivityAtAttributeZero;
				public final ForgeConfigSpec.DoubleValue productivityAtAttributeHundred;
				public final ForgeConfigSpec.DoubleValue maximumProficiency;
				public final ForgeConfigSpec.DoubleValue bonusAtMaximumProficiency;
				public final ForgeConfigSpec.DoubleValue minimumResidentProductivity;
				public final ForgeConfigSpec.DoubleValue maximumResidentProductivity;
				public final ForgeConfigSpec.DoubleValue healthWeight;
				public final ForgeConfigSpec.DoubleValue mentalWeight;
				public final ForgeConfigSpec.DoubleValue strengthWeight;
				public final ForgeConfigSpec.DoubleValue intelligenceWeight;
				public final ForgeConfigSpec.DoubleValue assignmentBasePriority;
				public final ForgeConfigSpec.DoubleValue assignmentPenaltyPerWorker;
				public final ForgeConfigSpec.DoubleValue assignmentFillRatioBonus;

				Mining(ForgeConfigSpec.Builder builder) {
					builder.push("Mining");
					baseOutputPerStandardWorkerDay = builder
						.comment("Base mining output in item units per standard worker per Minecraft day.")
						.comment("A standard worker has all four attributes at 50 and zero mining proficiency.")
						.comment("1 item unit is one item stored in town storage; fractional units are retained.")
						.defineInRange("baseOutputPerStandardWorkerDay",
							TownModelParameters.Defaults.MINING_BASE_OUTPUT_PER_SWE_DAY,
							0d, 1000000d);
					floorBlocksPerWorkerSlot = builder
						.comment("Effective floor area required for one mining-base worker slot, in blocks per worker.")
						.comment("Space rating multiplies effective floor area before slots are calculated.")
						.defineInRange("floorBlocksPerWorkerSlot",
							TownModelParameters.Defaults.MINING_FLOOR_BLOCKS_PER_WORKER_SLOT,
							0.01d, 1000000d);
					minimumWorkerSlots = builder
						.comment("Minimum worker slots granted to every structurally valid mining base, in workers.")
						.defineInRange("minimumWorkerSlots",
							TownModelParameters.Defaults.MINING_MINIMUM_WORKER_SLOTS,
							0, 4096);
					connectionRadiusBlocks = builder
						.comment("Maximum straight-line distance from a mining base to an assigned mining camp, in blocks.")
						.defineInRange("connectionRadiusBlocks",
							TownModelParameters.Defaults.MINING_CONNECTION_RADIUS_BLOCKS,
							0, 32000);

					builder.push("Resident Productivity");
					productivityAtAttributeZero = builder
						.comment("Relative mining productivity at weighted attribute 0 and proficiency 0.")
						.defineInRange("productivityAtAttributeZero",
							TownModelParameters.Defaults.MINING_PRODUCTIVITY_AT_ATTRIBUTE_ZERO,
							0d, 100d);
					productivityAtAttributeHundred = builder
						.comment("Relative mining productivity at weighted attribute 100 and proficiency 0.")
						.comment("Linear interpolation makes weighted attribute 50 equal 1.0 with the defaults.")
						.defineInRange("productivityAtAttributeHundred",
							TownModelParameters.Defaults.MINING_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED,
							0d, 100d);
					maximumProficiency = builder
						.comment("Profession proficiency that grants the full configured productivity bonus.")
						.defineInRange("maximumProficiency",
							TownModelParameters.Defaults.MINING_MAXIMUM_PROFICIENCY, 1d, 100d);
					bonusAtMaximumProficiency = builder
						.comment("Additive relative productivity granted at maximum mining proficiency.")
						.defineInRange("bonusAtMaximumProficiency",
							TownModelParameters.Defaults.MINING_BONUS_AT_MAXIMUM_PROFICIENCY,
							0d, 100d);
					minimumResidentProductivity = builder
						.comment("Minimum final mining productivity in standard-worker units.")
						.defineInRange("minimumResidentProductivity",
							TownModelParameters.Defaults.MINING_MINIMUM_PRODUCTIVITY, 0d, 100d);
					maximumResidentProductivity = builder
						.comment("Maximum final mining productivity in standard-worker units.")
						.defineInRange("maximumResidentProductivity",
							TownModelParameters.Defaults.MINING_MAXIMUM_PRODUCTIVITY, 0d, 100d);
					builder.pop();

					builder.push("Attribute Weights");
					healthWeight = defineMiningAttributeWeight(builder, "healthWeight", "health",
						TownModelParameters.Defaults.MINING_HEALTH_WEIGHT);
					mentalWeight = defineMiningAttributeWeight(builder, "mentalWeight", "mental",
						TownModelParameters.Defaults.MINING_MENTAL_WEIGHT);
					strengthWeight = defineMiningAttributeWeight(builder, "strengthWeight", "strength",
						TownModelParameters.Defaults.MINING_STRENGTH_WEIGHT);
					intelligenceWeight = defineMiningAttributeWeight(builder, "intelligenceWeight", "intelligence",
						TownModelParameters.Defaults.MINING_INTELLIGENCE_WEIGHT);
					builder.pop();

					builder.push("Worker Assignment");
					assignmentBasePriority = builder
						.comment("Deprecated compatibility value; the town-level staffing queue now controls assignment and ignores this setting.")
						.defineInRange("basePriority",
							TownModelParameters.Defaults.MINING_ASSIGNMENT_BASE_PRIORITY,
							-1000000d, 1000000d);
					assignmentPenaltyPerWorker = builder
						.comment("Deprecated compatibility value; ignored by the current staffing planner.")
						.defineInRange("penaltyPerWorker",
							TownModelParameters.Defaults.MINING_ASSIGNMENT_PENALTY_PER_WORKER,
							0d, 1000000d);
					assignmentFillRatioBonus = builder
						.comment("Deprecated compatibility value; ignored by the current staffing planner.")
						.defineInRange("fillRatioBonus",
							TownModelParameters.Defaults.MINING_ASSIGNMENT_FILL_RATIO_BONUS,
							-1000000d, 1000000d);
					builder.pop();
					builder.pop();
				}

				private static ForgeConfigSpec.DoubleValue defineMiningAttributeWeight(
						ForgeConfigSpec.Builder builder, String key, String attributeName, double defaultValue) {
					return builder
						.comment("Relative dimensionless weight of " + attributeName + " in mining productivity.")
						.comment("Weights are normalized by their sum; setting every weight to zero uses an equal-weight average.")
						.defineInRange(key, defaultValue, 0d, 1000d);
				}
			}
			public static class Resource{
				/**
				 * @deprecated Use {@link #oreReservePerChunk}. Kept as a Java alias
				 * for source compatibility; both fields refer to the same config value.
				 */
				@Deprecated
				public final ForgeConfigSpec.ConfigValue<Double> oreCount;
				/**
				 * @deprecated Use {@link #oreRecoveryPerChunkDay}. Kept as a Java
				 * alias for source compatibility; both fields refer to the same config value.
				 */
				@Deprecated
				public final ForgeConfigSpec.ConfigValue<Double> oreRecovery;
				public final ForgeConfigSpec.ConfigValue<Double> oreReservePerChunk;
				public final ForgeConfigSpec.ConfigValue<Double> oreRecoveryPerChunkDay;
				public final ForgeConfigSpec.ConfigValue<Double> treeCount;
				public final ForgeConfigSpec.ConfigValue<Double> treeRecovery;
				/**
				 * @deprecated Use {@link #huntReservePerSquareBlock}. Kept as a Java
				 * alias for source compatibility; both fields refer to the same config value.
				 */
				@Deprecated
				public final ForgeConfigSpec.ConfigValue<Double> huntCount;
				/**
				 * @deprecated Use {@link #huntRecoveryPerSquareBlockDay}. Kept as a
				 * Java alias for source compatibility; both fields refer to the same config value.
				 */
				@Deprecated
				public final ForgeConfigSpec.ConfigValue<Double> huntRecovery;
				public final ForgeConfigSpec.ConfigValue<Double> huntReservePerSquareBlock;
				public final ForgeConfigSpec.ConfigValue<Double> huntRecoveryPerSquareBlockDay;
				public final ForgeConfigSpec.ConfigValue<Double> poiCount;
				public final ForgeConfigSpec.ConfigValue<Double> poiRecovery;
				public final ForgeConfigSpec.ConfigValue<Double> salvageCount;
				public final ForgeConfigSpec.ConfigValue<Double> salvageRecovery;
				Resource(ForgeConfigSpec.Builder builder) {
					builder.push("Pick Resource");
					oreReservePerChunk = builder
						.comment("Total extractable ore reserve in ore/item units per active mining chunk.")
						.comment("The TOML key keeps its legacy name orePerSq for compatibility, but current chunk-tracked mining does not multiply it by chunk area.")
						.defineInRange("orePerSq", TownModelParameters.Defaults.ORE_RESERVE_PER_CHUNK, 0d, 1000000d);
					oreRecoveryPerChunkDay = builder
						.comment("Configured ore recovery in ore units per chunk per Minecraft day.")
						.comment("Current chunk-tracked mining does not yet apply this recovery value.")
						.defineInRange("orePerDay", TownModelParameters.Defaults.ORE_RECOVERY_PER_CHUNK_DAY, 0d, 1000000d);
					oreCount = oreReservePerChunk;
					oreRecovery = oreRecoveryPerChunkDay;
					treeCount=builder.comment("Tree Count per block squared")
						.defineInRange("treePerSq", 0.4d, 0d, 1000000d);
					treeRecovery=builder.comment("Tree Recovery per block per day")
						.defineInRange("treePerDay", 0.0025d, 0d, 1000000d);
					huntReservePerSquareBlock = builder
						.comment("Hunt terrain-resource density in loot-roll units per square block.")
						.comment("The TOML key keeps its legacy name huntPerSq for compatibility.")
						.defineInRange("huntPerSq", TownModelParameters.Defaults.HUNT_RESERVE_PER_SQUARE_BLOCK, 0d, 1000000d);
					huntRecoveryPerSquareBlockDay = builder
						.comment("Hunt terrain-resource recovery density in hunt units per square block per Minecraft day.")
						.comment("Recovery is applied over the resource system's currently depleted radius.")
						.defineInRange("huntPerDay", TownModelParameters.Defaults.HUNT_RECOVERY_PER_SQUARE_BLOCK_DAY, 0d, 1000000d);
					huntCount = huntReservePerSquareBlock;
					huntRecovery = huntRecoveryPerSquareBlockDay;
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
			public final Hunting HUNTING;
			public final Housing HOUSING;
			public final BuildingScoring BUILDING_SCORING;
			public final GeneratorT1 GENERATOR_T1;
			public final Mining MINING;
			public final ResidentRules RESIDENT_RULES;
			public final ResidentProgression RESIDENT_PROGRESSION;
			public final ResidentGeneration RESIDENT_GENERATION;
			public final RefugeeSpawn REFUGEE_SPAWN;
			public final ResidentAging RESIDENT_AGING;
			public final Resource RESOURCE;
			public final Observation OBSERVATION;
			Town(ForgeConfigSpec.Builder builder) {
				builder.push("Town");
				enableTownTick = builder.comment("Enables town tick every second.")
					.comment("This tick includes the running of town worker blocks.")
					.define("enableTownTick", true);
				townUpdateIntervalGameTicks = builder
					.comment("Interval between online town generator updates, in game ticks.")
					.comment("The update consumes the same total fuel as per-tick processing because fuel balances carry across batches.")
					.defineInRange("townUpdateIntervalGameTicks",
						TownModelParameters.Defaults.TOWN_UPDATE_INTERVAL_GAME_TICKS,
						1, 1200);
				enableTownTickMorning = builder.comment("Enables town tick in the morning of each days.")
					.comment("This tick includes the refresh of some town things, like house allocating, checking overlap of buildings, work assigning...")
					.define("enableTownTickMorning", true);
				maxVisibleCitizensPerPlayer = builder
					.comment("Maximum citizens synchronized and rendered for one player across awake and valid-bed sleepers.")
					.comment("Zero hides all citizens for every player.")
					.defineInRange("maxVisibleCitizensPerPlayer", 128, 0, 4096);
				maxVisibleCitizensPerServer = builder
					.comment("Maximum citizen render relations across all players and dimensions on this server.")
					.comment("The same citizen visible to two players consumes two slots. Zero hides all citizens server-wide.")
					.defineInRange("maxVisibleCitizensPerServer", 1024, 0, 65536);
				GENERATOR_T1 = new GeneratorT1(builder);
				OBSERVATION = new Observation(builder);
				BUILDING_SCORING = new BuildingScoring(builder);
				HOUSING = new Housing(builder);
				RESIDENT_RULES = new ResidentRules(builder);
				RESIDENT_PROGRESSION = new ResidentProgression(builder);
				RESIDENT_GENERATION = new ResidentGeneration(builder);
				REFUGEE_SPAWN = new RefugeeSpawn(builder);
				RESIDENT_AGING = new ResidentAging(builder);
				HUNTING = new Hunting(builder);
				MINING = new Mining(builder);
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
			public final ForgeConfigSpec.ConfigValue<Boolean> specialMode;

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
				specialMode = builder
						.comment("Enables a very special mode when the time is right.")
						.define("specialMode", true);
				builder.pop();
			}
		}

		public static class StorageDrawers {
			public final ForgeConfigSpec.IntValue inputCooldownTicks;
			public final ForgeConfigSpec.IntValue outputCooldownTicks;

			StorageDrawers(ForgeConfigSpec.Builder builder) {
				builder.push("Storage Drawers");
				inputCooldownTicks = builder
						.comment("Minimum ticks between successful automated item insertions into a drawer controller or slave.")
						.defineInRange("inputCooldownTicks", 10, 1, Integer.MAX_VALUE);
				outputCooldownTicks = builder
						.comment("Minimum ticks between successful automated item extractions from a drawer controller or slave.")
						.defineInRange("outputCooldownTicks", 40, 1, Integer.MAX_VALUE);
				builder.pop();
			}
		}

		/**
		 * 「雪原深处的好奇心」Boss 配置。
		 * <p>
		 * Config for the "Curiosity of the Deep Frostland" boss. See
		 * docs/boss/curiosity-boss-design.md.
		 */
		public static class Curiosity {
			public final ForgeConfigSpec.IntValue arenaRadius;
			public final ForgeConfigSpec.IntValue lingerRadius;
			public final ForgeConfigSpec.IntValue lingerSeconds;
			public final ForgeConfigSpec.IntValue escapeRadius;
			public final ForgeConfigSpec.IntValue escapeSeconds;
			public final ForgeConfigSpec.IntValue huntDurationTicks;
			public final ForgeConfigSpec.IntValue mazeDurationTicks;
			public final ForgeConfigSpec.IntValue mazeRaiseTicks;
			public final ForgeConfigSpec.IntValue risingTicks;
			public final ForgeConfigSpec.IntValue burrowTicks;
			public final ForgeConfigSpec.IntValue coldTier1;
			public final ForgeConfigSpec.IntValue coldTier2;
			public final ForgeConfigSpec.IntValue coldPerRound;
			public final ForgeConfigSpec.IntValue coldCap;
			public final ForgeConfigSpec.DoubleValue trackerSpeed;
			public final ForgeConfigSpec.DoubleValue trackerSpeedPerRound;
			public final ForgeConfigSpec.DoubleValue trackerSpeedCap;
			public final ForgeConfigSpec.BooleanValue powderSnowEnabled;
			public final ForgeConfigSpec.IntValue powderSnowIntervalTicks;
			public final ForgeConfigSpec.IntValue powderSnowMaxPatches;
			public final ForgeConfigSpec.IntValue moundIntervalTicks;
			public final ForgeConfigSpec.IntValue moundLifetimeTicks;
			public final ForgeConfigSpec.IntValue coreHealth;
			public final ForgeConfigSpec.IntValue coreBurnTicks;
			public final ForgeConfigSpec.IntValue mazeCells;
			public final ForgeConfigSpec.IntValue spawnWeight;
			public final ForgeConfigSpec.ConfigValue<List<? extends String>> spawnBiomes;
			public final ForgeConfigSpec.IntValue oreFrostDropCount;
			public final ForgeConfigSpec.IntValue oreFrostDropXp;
			public final ForgeConfigSpec.BooleanValue bossMusic;

			Curiosity(ForgeConfigSpec.Builder builder) {
				builder.push("Curiosity Boss");
				arenaRadius = builder.comment("Radius of the boss arena in blocks.")
						.defineInRange("arenaRadius", 24, 8, 96);
				lingerRadius = builder.comment("Radius in which a lingering player wakes the boss up.")
						.defineInRange("lingerRadius", 12, 2, 64);
				lingerSeconds = builder.comment("How long a player must linger to wake the boss up.")
						.defineInRange("lingerSeconds", 5, 1, 600);
				escapeRadius = builder.comment("Radius outside which the boss resets (players fled or all died).")
						.defineInRange("escapeRadius", 40, 16, 128);
				escapeSeconds = builder.comment("Grace period before reset when nobody is in the arena.")
						.defineInRange("escapeSeconds", 10, 1, 600);
				huntDurationTicks = builder.comment("Duration of the underground hunting phase in ticks.")
						.defineInRange("huntDurationTicks", 1200, 200, 24000);
				mazeDurationTicks = builder.comment("Time to find and burn the exposed core in ticks.")
						.defineInRange("mazeDurationTicks", 1200, 200, 24000);
				mazeRaiseTicks = builder.comment("Duration of the maze rising animation in ticks.")
						.defineInRange("mazeRaiseTicks", 100, 20, 400);
				risingTicks = builder.comment("Duration of the rising intro in ticks.")
						.defineInRange("risingTicks", 60, 10, 400);
				burrowTicks = builder.comment("Duration of the burrow transition in ticks.")
						.defineInRange("burrowTicks", 40, 10, 200);
				coldTier1 = builder.comment("Cold field value during rising/hunt (negative).")
						.defineInRange("coldTier1", -15, -200, 0);
				coldTier2 = builder.comment("Cold field value during the maze phase (negative).")
						.defineInRange("coldTier2", -30, -200, 0);
				coldPerRound = builder.comment("Extra cold added per round after each burrow (negative).")
						.defineInRange("coldPerRound", -15, -200, 0);
				coldCap = builder.comment("Lower bound of the cold field value (negative).")
						.defineInRange("coldCap", -75, -200, 0);
				trackerSpeed = builder.comment("Tracker speed in blocks per tick (player sprint is about 0.28).")
						.defineInRange("trackerSpeed", 0.24, 0.05, 2.0);
				trackerSpeedPerRound = builder.comment("Tracker speed bonus per round.")
						.defineInRange("trackerSpeedPerRound", 0.03, 0.0, 1.0);
				trackerSpeedCap = builder.comment("Tracker speed upper bound.")
						.defineInRange("trackerSpeedCap", 0.45, 0.05, 3.0);
				powderSnowEnabled = builder.comment("Whether the tracker leaves powder snow patches.")
						.define("powderSnowEnabled", true);
				powderSnowIntervalTicks = builder.comment("Ticks between powder snow patches.")
						.defineInRange("powderSnowIntervalTicks", 30, 5, 400);
				powderSnowMaxPatches = builder.comment("Max powder snow blocks kept at once (rolling window).")
						.defineInRange("powderSnowMaxPatches", 40, 4, 256);
				moundIntervalTicks = builder.comment("Ticks between surface mound spawns.")
						.defineInRange("moundIntervalTicks", 10, 2, 100);
				moundLifetimeTicks = builder.comment("Lifetime of a surface mound in ticks.")
						.defineInRange("moundLifetimeTicks", 10, 2, 100);
				coreHealth = builder.comment("Health of the exposed core.")
						.defineInRange("coreHealth", 20, 1, 200);
				coreBurnTicks = builder.comment("Burn ticks before the ignited core disperses.")
						.defineInRange("coreBurnTicks", 60, 10, 400);
				mazeCells = builder.comment("Maze grid size in cells (cell is 3 blocks, wall is 1).")
						.defineInRange("mazeCells", 11, 5, 21);
				spawnWeight = builder.comment("Natural spawn weight. High default for testing; lower before release.")
						.defineInRange("spawnWeight", 1, 0, 1000);
				spawnBiomes = builder.comment("Biome IDs where the boss may spawn. Default: snowy plains only; the pack adds custom snowy biomes here.")
						.defineListAllowEmpty("spawnBiomes",
								() -> new ArrayList<>(List.of("minecraft:snowy_plains")), o -> o instanceof String);
				oreFrostDropCount = builder.comment("Number of random condensed ore balls dropped on dispersal.")
						.defineInRange("oreFrostDropCount", 24, 0, 256);
				oreFrostDropXp = builder.comment("Experience orbs awarded on dispersal.")
						.defineInRange("oreFrostDropXp", 50, 0, 10000);
				bossMusic = builder.comment("Play the boss music (the_fall_of_arcana) during the fight.")
						.define("bossMusic", true);
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
		public final StorageDrawers STORAGE_DRAWERS;
		public final Misc MISC;
		public final Curiosity CURIOSITY;

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
			STORAGE_DRAWERS = new StorageDrawers(builder);
			MISC = new Misc(builder);
			CURIOSITY = new Curiosity(builder);
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

	static final boolean specialDay = MonthDay.of(4, 1).equals(MonthDay.now());
	public static boolean isSpecialMode() {
		return specialDay && FHConfig.SERVER.MISC.specialMode.get();
	}
}
