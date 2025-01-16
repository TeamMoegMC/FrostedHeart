package com.teammoeg.chorda;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

//@Mod(Chorda.MODID)
public class Chorda {
    public static final String MODID = "chorda";
    public static final String MODNAME = "Chorda";
    // Logger
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
    public static final Marker VERSION_CHECK = MarkerManager.getMarker("Version Check");
    public static final Marker INIT = MarkerManager.getMarker("Init");
    public static final Marker SETUP = MarkerManager.getMarker("Setup");
    public static final Marker COMMON_INIT = MarkerManager.getMarker("Common").addParents(INIT);
    public static final Marker CLIENT_INIT = MarkerManager.getMarker("Client").addParents(INIT);
    public static final Marker COMMON_SETUP = MarkerManager.getMarker("Common").addParents(SETUP);
    public static final Marker CLIENT_SETUP = MarkerManager.getMarker("Client").addParents(SETUP);

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public Chorda() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;

        // Config
        LOGGER.info(COMMON_INIT, "Loading Config");
        ChordaConfig.register();

        // Init
        LOGGER.info(COMMON_INIT, "Initializing " + MODNAME);

        // Compat init
        LOGGER.info(COMMON_INIT, "Initializing Mod Compatibilities");

        // Deferred Registration
        // Order doesn't matter here, as that's why we use deferred registers
        // See ForgeRegistries for more info
        LOGGER.info(COMMON_INIT, "Registering Deferred Registers");

        // Forge bus
        LOGGER.info(COMMON_INIT, "Registering Forge Event Listeners");

        // Mod bus
        LOGGER.info(COMMON_INIT, "Registering Mod Event Listeners");
        mod.addListener(this::setup);
        mod.addListener(this::processIMC);
        mod.addListener(this::enqueueIMC);
        mod.addListener(this::loadComplete);

        // Client setup
        LOGGER.info(COMMON_INIT, "Proceeding to Client Initialization");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ChordaClient.init();
        }
    }

    /**
     * Setup stuff that requires deferred registers to be filled.
     * @param event The event
     */
    private void setup(final FMLCommonSetupEvent event) {
        ChordaNetwork.register();
    }

    /**
     * Enqueue Inter-Mod Communication
     * @param event The event
     */
    private void enqueueIMC(final InterModEnqueueEvent event) {

    }

    /**
     * Process Inter-Mod Communication
     * @param event The event
     */
    private void processIMC(final InterModProcessEvent event) {

    }

    /**
     * Stuff that needs to be done after everything is loaded.
     * In general, not used.
     * @param event The event
     */
    private void loadComplete(FMLLoadCompleteEvent event) {

    }

}
