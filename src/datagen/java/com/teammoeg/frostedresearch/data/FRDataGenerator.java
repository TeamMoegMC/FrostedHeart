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

//package com.teammoeg.frostedresearch.data;
//
//import com.teammoeg.frostedheart.FHMain;
//import com.teammoeg.frostedresearch.FRMain;
//
//import net.minecraft.data.DataGenerator;
//import net.minecraftforge.data.event.GatherDataEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//public class FRDataGenerator {
//	@SubscribeEvent
//	public static void gatherData(GatherDataEvent event) {
//		DataGenerator gen = event.getGenerator();
//
//		gen.addProvider(event.includeServer(), new FRBlockTagProvider(gen, FHMain.MODID, event.getExistingFileHelper(), event.getLookupProvider()));
//		gen.addProvider(event.includeServer(), new FRLootTableProvider(event.getGenerator().getPackOutput()));
//	}
//
//}