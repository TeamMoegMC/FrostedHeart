package com.teammoeg.frostedheart.client;

import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.screen.CrucibleScreen;
import com.teammoeg.frostedheart.client.screen.ElectrolyzerScreen;
import com.teammoeg.frostedheart.client.screen.GeneratorScreen;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FHClientModEvents {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Register screens
        registerIEScreen(new ResourceLocation(FHMain.MODID, "generator"), GeneratorScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "crucible"), CrucibleScreen::new);
        ScreenManager.registerFactory(FHTileTypes.ELECTROLYZER_CONTAINER.get(), ElectrolyzerScreen::new);
        // Register translucent render type
        RenderTypeLookup.setRenderLayer(FHContent.Blocks.rye_block, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.Blocks.electrolyzer, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.Multiblocks.generator, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.Multiblocks.crucible, RenderType.getCutoutMipped());
    }

    public static <C extends Container, S extends Screen & IHasContainer<C>> void
    registerIEScreen(ResourceLocation containerName, ScreenManager.IScreenFactory<C, S> factory) {
        ContainerType<C> type = (ContainerType<C>) GuiHandler.getContainerType(containerName);
        ScreenManager.registerFactory(type, factory);
    }
}
