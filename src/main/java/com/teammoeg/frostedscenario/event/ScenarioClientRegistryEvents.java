package com.teammoeg.frostedscenario.event;

import com.teammoeg.frostedscenario.FSMain;
import com.teammoeg.frostedscenario.client.FHScenarioClient;
import com.teammoeg.frostedscenario.client.gui.layered.font.KGlyphProvider;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FSMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class ScenarioClientRegistryEvents {

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(KGlyphProvider.INSTANCE);
    }
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent ev) {

		ev.register(FSMain.key_skipDialog.get());
	}
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent ev) {
		FHScenarioClient.setup();
	}
}
