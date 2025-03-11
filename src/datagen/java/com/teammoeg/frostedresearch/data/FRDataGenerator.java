package com.teammoeg.frostedresearch.data;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedresearch.FRMain;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FRDataGenerator {
	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator gen = event.getGenerator();
		gen.addProvider(event.includeServer(), new FRBlockTagProvider(gen, FHMain.MODID, event.getExistingFileHelper(), event.getLookupProvider()));
		gen.addProvider(event.includeServer(), new FRLootTableProvider(event.getGenerator().getPackOutput()));
	}

}