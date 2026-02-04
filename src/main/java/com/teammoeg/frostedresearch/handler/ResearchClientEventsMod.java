/*
 * Copyright (c) 2024 TeamMoeg
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
