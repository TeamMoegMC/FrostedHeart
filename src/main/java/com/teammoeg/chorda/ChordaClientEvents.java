package com.teammoeg.chorda;

import com.teammoeg.chorda.dataholders.client.CClientDataStorage;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public class ChordaClientEvents {
	@SubscribeEvent
	public static void clientTick(RenderTickEvent tick) {
		CClientDataStorage.checkAndSave();
	}
	
}
