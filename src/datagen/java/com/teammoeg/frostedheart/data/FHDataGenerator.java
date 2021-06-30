package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FHDataGenerator {
    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        ExistingFileHelper exHelper = event.getExistingFileHelper();
        if (event.includeServer()) {
            gen.addProvider(new FHRecipeProvider(gen));
            gen.addProvider(new FHMultiblockStatesProvider(gen, exHelper));
        }
    }
}
