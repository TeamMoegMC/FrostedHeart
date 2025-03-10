package com.teammoeg.frostedresearch.handler;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.events.ClientResearchStatusEvent;
import com.teammoeg.frostedresearch.gui.ResearchToast;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectCrafting;
import com.teammoeg.frostedresearch.research.effects.EffectShowCategory;

import net.minecraftforge.api.distmarker.Dist;
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
                ClientUtils.mc().getToasts().addToast(new ResearchToast(event.getResearch()));
        } else if (!event.isCompletion())
            return;
        for (Effect e : event.getResearch().getEffects())
            if (e instanceof EffectCrafting || e instanceof EffectShowCategory) {
                JEICompat.syncJEI();
                return;
            }
    }

}
