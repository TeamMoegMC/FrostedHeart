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
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.particles.FHParticleTypes;
import com.teammoeg.frostedheart.client.particles.SteamParticle;
import com.teammoeg.frostedheart.content.crucible.CrucibleScreen;
import com.teammoeg.frostedheart.content.generatort1.T1GeneratorScreen;
import com.teammoeg.frostedheart.content.generatort2.T2GeneratorScreen;
import com.teammoeg.frostedheart.content.heatervest.HeaterVestRenderer;
import com.teammoeg.frostedheart.content.radiator.RadiatorScreen;
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
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FHClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Register screens
        registerIEScreen(new ResourceLocation(FHMain.MODID, "generator"), T1GeneratorScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "generator_t2"), T2GeneratorScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "crucible"), CrucibleScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "radiator"), RadiatorScreen::new);
        // Register translucent render type
        RenderTypeLookup.setRenderLayer(FHContent.FHBlocks.rye_block, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHBlocks.white_turnip_block, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHMultiblocks.generator, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHMultiblocks.crucible, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHMultiblocks.generator_t2, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHMultiblocks.steam_turbine, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHBlocks.charger, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHBlocks.radiator, RenderType.getCutoutMipped());
        RenderTypeLookup.setRenderLayer(FHContent.FHBlocks.debug_heater, RenderType.getCutoutMipped());
        // Register layers
        Map<String, PlayerRenderer> skinMap = Minecraft.getInstance().getRenderManager().getSkinMap();
        PlayerRenderer render = skinMap.get("default");
        render.addLayer(new HeaterVestRenderer<>(render));
        render = skinMap.get("slim");
        render.addLayer(new HeaterVestRenderer<>(render));
    }

    public static <C extends Container, S extends Screen & IHasContainer<C>> void
    registerIEScreen(ResourceLocation containerName, ScreenManager.IScreenFactory<C, S> factory) {
        ContainerType<C> type = (ContainerType<C>) GuiHandler.getContainerType(containerName);
        ScreenManager.registerFactory(type, factory);
    }

    @SubscribeEvent
    public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particles.registerFactory(FHParticleTypes.STEAM.get(), SteamParticle.Factory::new);
    }
}
