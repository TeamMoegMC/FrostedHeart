package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // default tip
        if (!TipManager.INSTANCE.hasTip("default")) {
            Tip.builder("default").line(Components.str("Default Tip")).image(new ResourceLocation(FHMain.MODID, "textures/item/debug_item.png")).build().saveAsFile();
            TipManager.INSTANCE.loadFromFile();
        }
        TipManager.INSTANCE.display().clearRenderQueue();
        TipManager.INSTANCE.display().general("default");
//        if (Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.MUSIC) == 0) {
//            TipDisplayManager.displayTip("music_warning", false);
//        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TipManager.INSTANCE.display().clearRenderQueue();
        ClientWaypointManager.clear();
    }
}
