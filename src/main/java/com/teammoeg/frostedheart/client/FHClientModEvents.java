/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.client;

import blusunrize.immersiveengineering.common.gui.GuiHandler;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.render.FHBipedLayerRenderer;
import com.teammoeg.frostedheart.client.screen.CrucibleScreen;
import com.teammoeg.frostedheart.client.screen.GeneratorScreen;
import com.teammoeg.frostedheart.content.FHBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FHClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Register screens
        registerIEScreen(new ResourceLocation(FHMain.MODID, "generator"), GeneratorScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "crucible"), CrucibleScreen::new);
        // Register translucent render type
        RenderTypeLookup.setRenderLayer(FHBlocks.rye_block, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHBlocks.white_turnip_block, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHBlocks.Multi.generator, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHBlocks.Multi.crucible, RenderType.getCutoutMipped());
        // Register layers
        Map<String, PlayerRenderer> skinMap = Minecraft.getInstance().getRenderManager().getSkinMap();
        PlayerRenderer render = skinMap.get("default");
        render.addLayer(new FHBipedLayerRenderer<>(render));
        render = skinMap.get("slim");
        render.addLayer(new FHBipedLayerRenderer<>(render));
    }

    public static <C extends Container, S extends Screen & IHasContainer<C>> void
    registerIEScreen(ResourceLocation containerName, ScreenManager.IScreenFactory<C, S> factory) {
        ContainerType<C> type = (ContainerType<C>) GuiHandler.getContainerType(containerName);
        ScreenManager.registerFactory(type, factory);
    }
}
