/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.events;

import com.teammoeg.chorda.CompatModule;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.content.climate.network.C2SOpenClothesScreenMessage;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import com.teammoeg.frostedheart.content.health.network.C2SOpenNutritionScreenMessage;
import com.teammoeg.frostedheart.content.ui.wheelmenu.WheelMenuRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FHKeyHandler {

    @SubscribeEvent
    public static void onClientKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
   

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

            // open wheel menu
            if(FHKeyMappings.key_openWheelMenu.get().consumeClick()&&!WheelMenuRenderer.isOpened()) {
            	WheelMenuRenderer.open();
            }

            // drink water
            // see WaterClientEvents.java
        }
    }

    @SubscribeEvent
    public static void onClientScroll(InputEvent.MouseScrollingEvent event) {
    	if(WheelMenuRenderer.isOpened()) {
    		WheelMenuRenderer.scrollPage(event.getScrollDelta());
    		event.setCanceled(true);
    		
    	}
    }
}
