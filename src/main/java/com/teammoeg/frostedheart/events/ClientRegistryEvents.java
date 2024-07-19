/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.events;

import static net.minecraft.inventory.container.PlayerContainer.*;

import java.util.Map;
import java.util.function.Function;

import com.teammoeg.frostedheart.client.particles.WetSteamParticle;
import org.lwjgl.glfw.GLFW;

import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHContainer;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHParticleTypes;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.model.LiningFinalizedModel;
import com.teammoeg.frostedheart.client.model.LiningModel;
import com.teammoeg.frostedheart.client.particles.BreathParticle;
import com.teammoeg.frostedheart.client.particles.SteamParticle;
import com.teammoeg.frostedheart.compat.tetra.TetraClient;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorScreen;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorRenderer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorRenderer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorTileEntity;
import com.teammoeg.frostedheart.content.decoration.RelicChestScreen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.research.blocks.MechCalcRenderer;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatScreen;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaScreen;
import com.teammoeg.frostedheart.content.trade.gui.TradeScreen;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestRenderer;
import com.teammoeg.frostedheart.util.RegistryUtils;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.client.manual.ManualElementMultiblock;
import blusunrize.immersiveengineering.common.gui.GuiHandler;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.Tree;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRegistryEvents {
    private static Tree.InnerNode<ResourceLocation, ManualEntry> CATEGORY;

    public static void addManual() {
        ManualInstance man = ManualHelper.getManual();
        CATEGORY = man.getRoot().getOrCreateSubnode(new ResourceLocation(FHMain.MODID, "main"), 110);
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
            builder.addSpecialElement("generator", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.GENERATOR));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator"));
            man.addEntry(CATEGORY, builder.create(), 0);
        }
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
            builder.addSpecialElement("generator_2", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.GENERATOR_T2));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator_t2"));
            man.addEntry(CATEGORY, builder.create(), 1);
        }
    }

    public static <C extends Container, S extends BaseScreen> ScreenManager.IScreenFactory<C, MenuScreenWrapper<C>>
    FTBScreenFactory(Function<C, S> factory) {
        return (c, i, t) -> new MenuScreenWrapper<>(factory.apply(c), c, i, t).disableSlotDrawing();
    }
	public static KeyBinding key_skipDialog = new KeyBinding("key.frostedheart.skip_dialog", 
		GLFW.GLFW_KEY_Z, "key.categories.frostedheart");

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Register screens
        registerIEScreen(new ResourceLocation(FHMain.MODID, "generator"), MasterGeneratorScreen<T1GeneratorTileEntity>::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "generator_t2"), MasterGeneratorScreen<T2GeneratorTileEntity>::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "relic_chest"), RelicChestScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "draw_desk"), FTBScreenFactory(DrawDeskScreen::new));
        registerFTBScreen(FHContainer.TRADE_GUI.get(), TradeScreen::new);
        registerFTBScreen(FHContainer.HEAT_STAT.get(), HeatStatScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "sauna_vent"), SaunaScreen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "incubator"), IncubatorT1Screen::new);
        registerIEScreen(new ResourceLocation(FHMain.MODID, "heat_incubator"), IncubatorT2Screen::new);

        // Register translucent render type

        RenderTypeLookup.setRenderLayer(FHBlocks.rye_block.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.white_turnip_block.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.wolfberry_bush_block.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHMultiblocks.generator, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHMultiblocks.generator_t2, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.drawing_desk.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.charger.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.mech_calc.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.steam_core.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHMultiblocks.radiator, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.debug_heater.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.relic_chest.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.fluorite_ore.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.halite_ore.get(), RenderType.getCutout());
/*
        RenderTypeLookup.setRenderLayer(FHBlocks.blood_block, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.bone_block, RenderType.getCutout());
        //RenderTypeLookup.setRenderLayer(FHBlocks.desk, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.small_garage, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.package_block, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.pebble_block, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.odd_mark, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.wooden_box, RenderType.getCutout());*/
        ClientRegistry.bindTileEntityRenderer(FHTileTypes.GENERATOR_T1.get(), T1GeneratorRenderer::new);
        ClientRegistry.bindTileEntityRenderer(FHTileTypes.GENERATOR_T2.get(), T2GeneratorRenderer::new);
        ClientRegistry.bindTileEntityRenderer(FHTileTypes.MECH_CALC.get(), MechCalcRenderer::new);
        key_skipDialog.setKeyConflictContext(KeyConflictContext.IN_GAME);
		ClientRegistry.registerKeyBinding(key_skipDialog);
        // Register layers
        Map<String, PlayerRenderer> skinMap = Minecraft.getInstance().getRenderManager().getSkinMap();
        PlayerRenderer render = skinMap.get("default");
        render.addLayer(new HeaterVestRenderer<>(render));
        render = skinMap.get("slim");
        render.addLayer(new HeaterVestRenderer<>(render));
        addManual();
        if (ModList.get().isLoaded("tetra"))
            TetraClient.init();
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        for (ResourceLocation location : event.getModelRegistry().keySet()) {
            // Now find all armors
            ResourceLocation item = new ResourceLocation(location.getNamespace(), location.getPath());
            if (RegistryUtils.getItem(item) instanceof ArmorItem) {
                ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation(item, "inventory");
                IBakedModel model = event.getModelRegistry().get(itemModelResourceLocation);
                if (model == null) {
                    FHMain.LOGGER.warn("Did not find the expected vanilla baked model for " + item + " in registry");
                } else if (model instanceof LiningModel) {
                	FHMain.LOGGER.warn("Tried to replace " + item + " twice");
                } else {
                    // Replace the model with our IBakedModel
                    LiningModel customModel = new LiningModel(model);
                    event.getModelRegistry().put(itemModelResourceLocation, customModel);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTextureStitchEvent(TextureStitchEvent.Pre event) {
        if (event.getMap().getTextureLocation() == LOCATION_BLOCKS_TEXTURE) {
            event.addSprite(LiningFinalizedModel.buffCoatFeetTexture);
            event.addSprite(LiningFinalizedModel.buffCoatLegsTexture);
            event.addSprite(LiningFinalizedModel.buffCoatHelmetTexture);
            event.addSprite(LiningFinalizedModel.buffCoatTorsoTexture);
            event.addSprite(LiningFinalizedModel.gambesonLegsTexture);
            event.addSprite(LiningFinalizedModel.gambesonFeetTexture);
            event.addSprite(LiningFinalizedModel.gambesonHelmetTexture);
            event.addSprite(LiningFinalizedModel.gambesonTorsoTexture);
            event.addSprite(LiningFinalizedModel.kelpLiningLegsTexture);
            event.addSprite(LiningFinalizedModel.kelpLiningFeetTexture);
            event.addSprite(LiningFinalizedModel.kelpLiningHelmetTexture);
            event.addSprite(LiningFinalizedModel.kelpLiningTorsoTexture);
            event.addSprite(LiningFinalizedModel.strawLiningLegsTexture);
            event.addSprite(LiningFinalizedModel.strawLiningFeetTexture);
            event.addSprite(LiningFinalizedModel.strawLiningHelmetTexture);
            event.addSprite(LiningFinalizedModel.strawLiningTorsoTexture);
        }
    }

    @SubscribeEvent
    public static void provideTextures(final TextureStitchEvent.Pre event) {
        if (PlayerContainer.LOCATION_BLOCKS_TEXTURE.equals(event.getMap().getTextureLocation())) {
            Minecraft.getInstance().getResourceManager().getAllResourceLocations("textures/item/module", s -> s.endsWith(".png")).stream()
                    .filter(resourceLocation -> FHMain.MODID.equals(resourceLocation.getNamespace()))
                    // 9 is the length of "textures/" & 4 is the length of ".png"

                    .map(rl -> new ResourceLocation(rl.getNamespace(), rl.getPath().substring(9, rl.getPath().length() - 4)))
                    .peek(rl -> FHMain.LOGGER.info("stitching texture" + rl))
                    .forEach(event::addSprite);
        }
    }

    public static <C extends Container, S extends BaseScreen> void
    registerFTBScreen(ContainerType<C> type, Function<C, S> factory) {
        ScreenManager.registerFactory(type, FTBScreenFactory(factory));
    }

    public static <C extends Container, S extends Screen & IHasContainer<C>> void
    registerIEScreen(ResourceLocation containerName, ScreenManager.IScreenFactory<C, S> factory) {
        @SuppressWarnings("unchecked")
        ContainerType<C> type = (ContainerType<C>) GuiHandler.getContainerType(containerName);
        ScreenManager.registerFactory(type, factory);
    }

    @SubscribeEvent
    public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particles.registerFactory(FHParticleTypes.STEAM.get(), SteamParticle.Factory::new);
        Minecraft.getInstance().particles.registerFactory(FHParticleTypes.BREATH.get(), BreathParticle.Factory::new);
        Minecraft.getInstance().particles.registerFactory(FHParticleTypes.WET_STEAM.get(), WetSteamParticle.Factory::new);
    }

}
