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

package com.teammoeg.frostedresearch.handler;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.api.ClientKnowledgeDataAPI;
import com.teammoeg.frostedresearch.compat.ResearchJeiBridge;
import com.teammoeg.frostedresearch.events.ClientResearchStatusEvent;
import com.teammoeg.frostedresearch.gui.InsightOverlay;
import com.teammoeg.frostedresearch.gui.ResearchToast;
import com.teammoeg.frostedresearch.item.ResearchNotebookItem;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectCrafting;
import com.teammoeg.frostedresearch.research.effects.EffectShowCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ResearchClientEvents {

	public ResearchClientEvents() {
		// TODO Auto-generated constructor stub
	}
    @SubscribeEvent
    public static void onResearchStatus(ClientResearchStatusEvent event) {
        if (event.isStatusChanged()) {
            if (event.isCompletion())
                ClientUtils.getMc().getToasts().addToast(new ResearchToast(event.getResearch()));
        }
        for (Effect e : event.getResearch().getEffects())
            if (e instanceof EffectCrafting || e instanceof EffectShowCategory) {
                ResearchJeiBridge.sync();
                return;
            }
    }
    @SubscribeEvent
    public static void tickClient(ClientTickEvent event) {
    	if(ClientUtils.getPlayer()!=null&&InsightOverlay.INSTANCE!=null) {
    		InsightOverlay.INSTANCE.tick();
    	}
    }
    @SubscribeEvent
    public static void fireLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientKnowledgeDataAPI.reset();
        if(InsightOverlay.INSTANCE!=null)
        	InsightOverlay.INSTANCE.reset();

    }
    @SubscribeEvent
    public static void fireLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientKnowledgeDataAPI.reset();
    }

    /** Visible capture feedback; archiving still happens only after the server finishes the use. */
    @SubscribeEvent
    public static void renderNotebookCapture(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isUsingItem()
                || !(minecraft.player.getUseItem().getItem() instanceof ResearchNotebookItem)) return;
        int elapsed = ResearchNotebookItem.CAPTURE_TICKS - minecraft.player.getUseItemRemainingTicks();
        float progress = net.minecraft.util.Mth.clamp(
                (elapsed + event.getPartialTick()) / ResearchNotebookItem.CAPTURE_TICKS, 0.0F, 1.0F);
        int width = 92;
        int left = (event.getWindow().getGuiScaledWidth() - width) / 2;
        int top = event.getWindow().getGuiScaledHeight() - 48;
        event.getGuiGraphics().fill(left, top, left + width, top + 8, 0xCC27231D);
        event.getGuiGraphics().fill(left + 1, top + 1,
                left + 1 + Math.round((width - 2) * progress), top + 7, 0xFF3F756D);
        net.minecraft.network.chat.Component label = net.minecraft.network.chat.Component.translatable(
                "gui.frostedresearch.notebook.capturing");
        event.getGuiGraphics().drawString(minecraft.font, label,
                (event.getWindow().getGuiScaledWidth() - minecraft.font.width(label)) / 2,
                top - 11, 0xFFFFFFFF, true);
    }
}
