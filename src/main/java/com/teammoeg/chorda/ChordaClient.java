package com.teammoeg.chorda;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ChordaClient {

    public static void init() {
        // Client only init
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Initializing client");

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Registering client forge event listeners");

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Registering client mod event listeners");

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Finished initializing client");
    }
}
