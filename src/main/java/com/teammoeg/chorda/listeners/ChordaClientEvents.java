package com.teammoeg.chorda.listeners;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.PartialTickTracker;
import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.chorda.dataholders.client.CClientDataStorage;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChordaClientEvents {
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public static void onClientRenderTick(TickEvent.RenderTickEvent ev) {
		if(ev.phase==Phase.START) {
			if(!Minecraft.getInstance().isPaused())
				PartialTickTracker.getInstance().advanceTimer();
		}
	}
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public static void onClientTick(TickEvent.ClientTickEvent ev) {
		if(ev.phase==Phase.START) {
			if(!Minecraft.getInstance().isPaused())
				PartialTickTracker.getInstance().tick();
			
		}else {
			CClientDataStorage.checkAndSave();
		}
	}

	
	

}
