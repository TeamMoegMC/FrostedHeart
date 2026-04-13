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

package com.teammoeg.chorda;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Chorda 配置管理类。定义客户端、通用和服务端三种配置。
 * 使用 Forge 的 {@link ForgeConfigSpec} 系统。
 * <p>
 * Configuration management class for Chorda. Defines client, common,
 * and server configurations using Forge's {@link ForgeConfigSpec} system.
 */
public class ChordaConfig {

    /**
     * 客户端配置。仅影响本地客户端的设置。
     * <p>
     * Client configuration. Settings that only affect the local client.
     */
    public static class Client {

		public final ForgeConfigSpec.BooleanValue enableShaderPackCompat;
        Client(ForgeConfigSpec.Builder builder) {

			enableShaderPackCompat = builder.comment("Enables shaderpack compatibility module, switch this off if your shader does not load correctly")
				.define("enableShaderCompatibility", true);
        }

    }

    /**
     * 通用配置。在客户端和服务端之间同步的设置。
     * <p>
     * Common configuration. Settings synchronized between client and server.
     */
    public static class Common {

        Common(ForgeConfigSpec.Builder builder) {

        }

    }

    /**
     * 服务端配置。仅在服务端生效的设置。
     * <p>
     * Server configuration. Settings that only take effect on the server side.
     */
    public static class Server {
        /** 每 tick 执行的调度任务数量 / Number of scheduled tasks to execute per tick */
        public final ForgeConfigSpec.ConfigValue<Double> taskPerTick;

        Server(ForgeConfigSpec.Builder builder) {
            taskPerTick = builder.comment("Range Detection tasks to execute per tick")
                    .defineInRange("taskPerTick", 1, 0.005, Integer.MAX_VALUE);
        }

    }

    /**
     * 向 Forge 注册所有配置类型。
     * <p>
     * Registers all config types with Forge.
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }

    public static final ForgeConfigSpec CLIENT_CONFIG;
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final Client CLIENT;
    public static final Common COMMON;
    public static final Server SERVER;

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
}
