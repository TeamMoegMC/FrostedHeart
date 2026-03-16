package com.teammoeg.chorda.client;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.cui.screenadapter.CUIOverlay;
import com.teammoeg.frostedheart.content.tips.client.gui.TipOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端Mod事件总线订阅类，负责注册客户端侧的GUI覆盖层（Overlay）。
 * 包括CUI虚拟鼠标覆盖层和提示信息覆盖层的注册。
 * <p>
 * Client-side mod event bus subscriber responsible for registering client GUI overlays.
 * Registers the CUI virtual mouse overlay and the tip notification overlay.
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("cui_virtual_mouse", CUIOverlay.VIRTUAL_MOUSE_OVERLAY);
        event.registerAboveAll("twr_tip", new CUIOverlay(TipOverlay.INSTANCE, true, CUIOverlay.whenScreenOpened));
    }
}
