package com.teammoeg.frostedheart.events;

import org.lwjgl.glfw.GLFW;

import com.teammoeg.chorda.CompatModule;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.content.climate.network.C2SOpenClothesScreenMessage;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import com.teammoeg.frostedheart.content.health.network.C2SOpenNutritionScreenMessage;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FHKeyHandler {

    @SubscribeEvent
    public static void onClientKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            // skip scenario dialog
            if (FHKeyMappings.key_skipDialog.get().consumeClick()) {
                if (ClientScene.INSTANCE != null)
                    ClientScene.INSTANCE.sendContinuePacket(true);
                //event.setCanceled(true);
            }

            // toggle infrared view
            if (CompatModule.isLdLibLoaded() && FHKeyMappings.key_InfraredView.get().consumeClick()) {
                InfraredViewRenderer.toggleInfraredView();
            }

            // open nutrition screen
            if (FHKeyMappings.key_health.get().consumeClick()) {
                FHNetwork.INSTANCE.sendToServer(new C2SOpenNutritionScreenMessage());
            }

            // open clothes screen
            if(FHKeyMappings.key_clothes.get().consumeClick()) {
                FHNetwork.INSTANCE.sendToServer(new C2SOpenClothesScreenMessage());
            }
            if(FHKeyMappings.key_openWheelMenu.get().consumeClick()) {
            	WheelMenuRenderer.open();
            }
        }
    }
}
