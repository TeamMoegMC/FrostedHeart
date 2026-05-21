package com.teammoeg.frostedscenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.client.CInputHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FSMain.MODID)
public class FSMain {
    // CConstants
    public static final String MODID = "frostedscenario";
    public static final String ALIAS = "scenario";
    public static final String MODNAME = "Frosted Scenario";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);

    public static Lazy<KeyMapping> key_skipDialog =CInputHelper.createKey(MODID,"skip_dialog",GLFW.GLFW_KEY_Z,KeyConflictContext.IN_GAME);
	public FSMain() {
	    IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        CompatModule.enableCompatModule();
        FSCapabilities.setup();
        FSSpecialDataTypes.init();
        mod.addListener(this::setup);
        FSConfig.register();
	}

	public static ResourceLocation rl(String path) {
		return new ResourceLocation(MODID,path);
	}
    private void setup(final FMLCommonSetupEvent event) {
        
        FSNetwork.INSTANCE.register();
        FHScenario.setup();
    }
}