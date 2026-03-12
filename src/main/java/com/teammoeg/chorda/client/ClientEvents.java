package com.teammoeg.chorda.client;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.cui.screenadapter.CUIOverlay;
import com.teammoeg.frostedheart.content.tips.client.gui.TipOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("cui_virtual_mouse", CUIOverlay.VIRTUAL_MOUSE_OVERLAY);
        event.registerAboveAll("twr_tip", new CUIOverlay(TipOverlay.INSTANCE, true, CUIOverlay.whenScreenOpened));
    }
}
