/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.util.constants.FHTemperatureDifficulty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FHConfig {

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
        public final ForgeConfigSpec.BooleanValue renderTips;
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
                    .comment("X Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used. ")
                    .defineInRange("tempOrbOffsetX", 0, -4096, 4096);
            tempOrbOffsetY = builder
                    .comment("Y Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used.  ")
                    .defineInRange("tempOrbOffsetY", 0, -4096, 4096);
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
                    .defineInRange("snowDensity", 15, 1, 15);
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

            builder.push("Tips and Waypoints");
            enableWaypoint = builder
                    .comment("Enables the waypoints rendering. ")
                    .define("enableWaypoint", true);
            renderTips = builder.comment("Enables the tips rendering. ")
                    .define("renderTips", true);
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
            scenarioRenderQuality = builder.comment("Scenario 2d content rendering quality, internal resolution=2^(config value)*1024, 2d contents are rendered on cpu, higher quality may cause lag")
                    .defineInRange("scenarioRenderQuality", 2, 0, 16);
            scenarioRenderThread = builder.comment("Scenario rendering thread")
                    .defineInRange("scenarioRenderThread", 2, 1, 16);
            infraredViewUBOOffset = builder.comment("The binding offset of the UBO for the infrared view shader.")
                    .comment("Partial shaders and mods may occupy the position as well.")
                    .comment("We will use default offset (7) for some known mods here. However, it is not guaranteed to be always compatible with all mods / shaders.")
                    .comment("In this case, player have to modify the config to specify the offset.")
                    .comment("No worries, from my experience, offset 7 is compatible with 99% mods / shaders.")
                    .defineInRange("infraredViewUBOOffset", 7, 0, Integer.MAX_VALUE);
            builder.pop();

        }

        public int getScenarioScale() {
            return 1 << scenarioRenderQuality.get();
        }
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue enablesTemperatureForecast;
        public final ForgeConfigSpec.BooleanValue forceEnableTemperatureForecast;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blackmods;
        public final ForgeConfigSpec.BooleanValue enableDailyKitchen;
        public final ForgeConfigSpec.ConfigValue<Double> steamCoreMaxPower;
        public final ForgeConfigSpec.ConfigValue<Double> steamCorePowerIntake;
        public final ForgeConfigSpec.ConfigValue<Double> steamCoreGeneratedSpeed;
        public final ForgeConfigSpec.ConfigValue<Double> steamCoreCapacity;
        public final ForgeConfigSpec.ConfigValue<Double> flintIgnitionChance;
        public final ForgeConfigSpec.ConfigValue<Double> stickIgnitionChance;
        public final ForgeConfigSpec.ConfigValue<Double> consumeChanceWhenIgnited;
        public final ForgeConfigSpec.ConfigValue<Integer> simulationRange;
        public final ForgeConfigSpec.ConfigValue<Integer> simulationParticles;
        public final ForgeConfigSpec.ConfigValue<Integer> simulationDivision;
        public final ForgeConfigSpec.ConfigValue<Double> simulationParticleInitialSpeed;
        public final ForgeConfigSpec.ConfigValue<Integer> simulationParticleLife;

        Common(ForgeConfigSpec.Builder builder) {
            builder.push("Weather Forecast");
            enablesTemperatureForecast = builder
                    .comment("Enables the weather forecast system. ")
                    .define("enablesTemperatureForecast", true);
            forceEnableTemperatureForecast = builder
                    .comment("Forces the weather forecast system to be enabled regardless of scenario. ")
                    .define("forceEnableTemperatureForecast", false);
            builder.pop();

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

            builder.push("Fire Ignition");
            flintIgnitionChance = builder.comment("The chance of igniting when using a flint and metal.")
                    .defineInRange("flintIgnitionChance", 0.1, 0, 1);
            stickIgnitionChance = builder.comment("The chance of igniting igniting when using a stick.")
                    .defineInRange("stickIgnitionChance", 0.05, 0, 1);
            consumeChanceWhenIgnited = builder.comment("The chance of consuming the item when ignited.")
                    .defineInRange("consumeChanceWhenIgnited", 0.1, 0, 1);
            builder.pop();

            // TODO: Check numerics range
            builder.push("Surrounding Temperature Simulation").comment("The simulator is used to simulate the temperature of the surrounding environment. Not recommended to change.");
            simulationRange = builder.comment("The range of the simulation.")
                    .defineInRange("simulationRange", 8, 1, 8);
            simulationParticles = builder.comment("The number of particles in the simulation.")
                    .defineInRange("simulationParticles", 100, 4168, 4168);
            simulationDivision = builder.comment("The number of divisions of unit square in the simulation.")
                    .defineInRange("simulationDivision", 10, 1, 100);
            simulationParticleInitialSpeed = builder.comment("The initial speed of the particles in the simulation.")
                    .defineInRange("simulationParticleInitialSpeed", 0.4f, 0.01f, 1f);
            simulationParticleLife = builder.comment("The life ticks of the particles in the simulation.")
                    .defineInRange("simulationParticleLife", 20, 1, 100);
            builder.pop();

            builder.push("Miscellaneous");
            blackmods = builder
                    .comment("BlackListed mods to kick player")
                    .defineList("Mod Blacklist", new ArrayList<>(), s -> true);
            enableDailyKitchen = builder
                    .comment("Enables sending wanted food message. ")
                    .define("enableDailyKitchen", true);
            builder.pop();

        }
    }

    public static class Server {
        public final ForgeConfigSpec.BooleanValue alwaysKeepInventory;
        public final ForgeConfigSpec.BooleanValue fixEssJeiIssue;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> developers;
        public final ForgeConfigSpec.EnumValue<FHTemperatureDifficulty> tdiffculty;
        public final ForgeConfigSpec.ConfigValue<Double> tempSpeed;
        public final ForgeConfigSpec.BooleanValue keepEquipments;
        public final ForgeConfigSpec.ConfigValue<Double> taskPerTick;
        public final ForgeConfigSpec.ConfigValue<Double> waterReducingRate;
        public final ForgeConfigSpec.IntValue weaknessEffectAmplifier;
        public final ForgeConfigSpec.BooleanValue resetWaterLevelInDeath;
        public final ForgeConfigSpec.BooleanValue enableSnowAccumulationDuringWeather;
        public final ForgeConfigSpec.IntValue snowAccumulationDifficulty;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> nonWinterBiomes;
        public final ForgeConfigSpec.BooleanValue invertNonWinterBiomes;
        public final ForgeConfigSpec.BooleanValue enableSnowAccumulationDuringWorldgen;

        Server(ForgeConfigSpec.Builder builder) {
            builder.push("Temperature");
            tdiffculty = builder.comment("Temperature System difficulty", "Easy=Strong body", "Normal=Average", "Hard=Reality", "Hardcore=Sick body")
                    .defineEnum("temperatureDifficulty", FHTemperatureDifficulty.Normal);
            tempSpeed = builder.comment("Modifier of body temperature change speed, This does not affect hypothermia temperature.")
                    .defineInRange("temperatureChangeRate", 0.5, 0, 20);
            builder.pop();

            builder.push("Water");
            waterReducingRate = builder.comment("finalReducingValue = basicValue * waterReducingRate.(DoubleValue)")
                    .defineInRange("waterReducingRate", 1.0D, 0d, 1000D);
            weaknessEffectAmplifier = builder.comment("It is the weakness effect amplifier of the effect punishment when player's water level is too low. -1 means canceling this effect. Default:0")
                    .defineInRange("weaknessEffectAmplifier", 0, -1, 999999);
            resetWaterLevelInDeath = builder.comment("It decides if players' water level would reset in death.")
                    .define("resetWaterLevelInDeath", true);
            builder.pop();

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
                            "terralith:yellowstone",
                            "terralith:volcanic_crater",
                            "terralith:volcanic_peaks",
                            "terralith:caldera"
                    ));
            invertNonWinterBiomes = builder.comment("If true, the 'nonWinterBiomes' config option will be interpreted as a list of winter biomes, and all others will be ignored.")
                    .define("invertNonWinterBiomes", false);
            enableSnowAccumulationDuringWorldgen = builder.comment("Enables snow accumulation during world generation.")
                    .define("enableSnowAccumulationDuringWorldgen", false);
            builder.pop();

            builder.push("Miscellaneous");
            alwaysKeepInventory = builder
                    .comment("Always keep inventory on death on every dimension and world")
                    .define("alwaysKeepInventory", false);
            keepEquipments = builder.comment("Instead of keeping all inventory, only keep equipments, curios and quickbar tools on death")
                    .define("keepEquipments", true);
            fixEssJeiIssue = builder
                    .comment("Fixes JEI and Bukkit server compat issue, don't touch unless you know what you are doing.")
                    .define("fixEssJeiIssue", true);
            taskPerTick = builder.comment("Range Detection tasks to execute per tick")
                    .defineInRange("taskPerTick", 1, 0.005, Integer.MAX_VALUE);
            developers = builder
                    .comment("Special array of players")
                    .defineList("Player Whitelist", DEFAULT_WHITELIST, s -> true);
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
            final Stream<ResourceLocation> stream = FHConfig.SERVER.nonWinterBiomes.get().stream().map(ResourceLocation::new);
            return FHConfig.SERVER.invertNonWinterBiomes.get() ? stream.anyMatch(name::equals) : stream.noneMatch(name::equals);
        }
        return false;
    }
}
