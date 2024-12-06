package com.teammoeg.frostedheart.content.water.renderer;

import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.color.item.ItemColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ColorEvent {
    public static final ItemColor CUP_ITEM = new WoodenCupColor();
    public static final ItemColor FLUID_BOTTLE_ITEM = new FluidBottleColor();

    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.getItemColors().register(CUP_ITEM, FHItems.wooden_cup_drink.get());
        event.getItemColors().register(FLUID_BOTTLE_ITEM, FHItems.fluid_bottle.get());
    }
}
