/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class FHConfig {

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, FHConfig.CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FHConfig.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FHConfig.SERVER_CONFIG);
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue enablesTemperatureOrb;
        public final ForgeConfigSpec.BooleanValue enableUI;
        public final ForgeConfigSpec.IntValue tempOrbOffsetX;
        public final ForgeConfigSpec.IntValue tempOrbOffsetY;
        public final ForgeConfigSpec.EnumValue<TempOrbPos> tempOrbPosition;
        public final ForgeConfigSpec.BooleanValue useFahrenheit;
        Client(ForgeConfigSpec.Builder builder) {
            enableUI = builder
                    .comment("Enables The Winter Rescue HUD. THIS IS MODPACK CORE FEATURE, DISABLING IS NOT RECOMMENDED. ")
                    .define("enableHUD", true);
            enablesTemperatureOrb = builder
                    .comment("Enables the temperature orb overlay. ")
                    .define("enableTemperatureOrb", true);

            useFahrenheit=builder.comment("Use Fahrenheit temperature instead of celsus.")
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
        }
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue enablesTemperatureForecast;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blackmods;
        Common(ForgeConfigSpec.Builder builder) {
            enablesTemperatureForecast = builder
                    .comment("Enables the weather forecast system. ")
                    .define("enablesTemperatureForecast", true);
            blackmods= builder
                    .comment("BlackListed mods to kick player")
                    .defineList("Mod Blacklist",new ArrayList<>(), s -> true);
        }
    }

    public static class Server {
        public final ForgeConfigSpec.BooleanValue alwaysKeepInventory;
        public final ForgeConfigSpec.BooleanValue fixEssJeiIssue;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> developers;
        public final ForgeConfigSpec.EnumValue<FHDifficulty> tdiffculty;
        public final ForgeConfigSpec.ConfigValue<Double> tempSpeed;
        public final ForgeConfigSpec.BooleanValue keepEquipments;
        public final ForgeConfigSpec.ConfigValue<Double> taskPerTick;
        Server(ForgeConfigSpec.Builder builder) {
            alwaysKeepInventory = builder
                    .comment("Always keep inventory on death on every dimension and world")
                    .define("alwaysKeepInventory", false);
            keepEquipments = builder.comment("Instead of keeping all inventory, only keep equipments, curios and quickbar tools on death")
                    .define("keepEquipments", true);
            fixEssJeiIssue = builder
                    .comment("Fixes JEI and Bukkit server compat issue, don't touch unless you know what you are doing.")
                    .define("fixEssJeiIssue", true);
            taskPerTick =builder.comment("Range Detection tasks to execute per tick")
            		.defineInRange("taskPerTick", 1, 0.005,Integer.MAX_VALUE);
            developers = builder
                    .comment("Special array of players")
                    .defineList("Player Whitelist", DEFAULT_WHITELIST, s -> true);
            tdiffculty = builder.comment("Temperature System difficulty", "Easy=Strong body", "Normal=Average", "Hard=Reality", "Hardcore=Sick body")
                    .defineEnum("temperatureDifficulty", FHDifficulty.Normal);
            tempSpeed = builder.comment("Modifier of body temperature change speed, This does not affect hypothermia temperature.")
                    .defineInRange("temperatureChangeRate", 0.5, 0, 20);
            

        }
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
        DEFAULT_WHITELIST.add("Dev");
    }

    public enum TempOrbPos {
        MIDDLE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;
    }
}
