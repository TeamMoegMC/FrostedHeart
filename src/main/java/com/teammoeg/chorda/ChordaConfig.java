package com.teammoeg.chorda;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ChordaConfig {

    public static class Client {

        Client(ForgeConfigSpec.Builder builder) {

        }

    }

    public static class Common {

        Common(ForgeConfigSpec.Builder builder) {

        }

    }

    public static class Server {
        public final ForgeConfigSpec.ConfigValue<Double> taskPerTick;

        Server(ForgeConfigSpec.Builder builder) {
            taskPerTick = builder.comment("Range Detection tasks to execute per tick")
                    .defineInRange("taskPerTick", 1, 0.005, Integer.MAX_VALUE);
        }

    }
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
