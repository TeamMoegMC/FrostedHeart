package com.teammoeg.frostedresearch.handler;


import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.blocks.MechCalcRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ResearchClientEventsMod {

	public ResearchClientEventsMod() {
		// TODO Auto-generated constructor stub
	}
	@SubscribeEvent
	public static void registerBERenders(RegisterRenderers event){
		FRMain.LOGGER.info("===========Dynamic Block Renderers========");
        event.registerBlockEntityRenderer(FRContents.BlockEntityTypes.MECH_CALC.get(), MechCalcRenderer::new);
	}
}
