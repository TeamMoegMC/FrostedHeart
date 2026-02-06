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
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuRenderer;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.events.ClientResearchStatusEvent;
import com.teammoeg.frostedresearch.gui.InsightOverlay;
import com.teammoeg.frostedresearch.gui.ResearchToast;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectCrafting;
import com.teammoeg.frostedresearch.research.effects.EffectShowCategory;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
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
                JEICompat.syncJEI();
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
        if(InsightOverlay.INSTANCE!=null)
        	InsightOverlay.INSTANCE.reset();

    }
}
