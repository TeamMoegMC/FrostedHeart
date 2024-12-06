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

import java.util.function.Function;

import com.teammoeg.frostedheart.*;
import com.teammoeg.frostedheart.base.creativeTab.CreativeTabItemHelper;
import com.teammoeg.frostedheart.base.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedheart.foundation.particles.WetSteamParticle;
import com.teammoeg.frostedheart.content.climate.client.SnowParticle;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorState;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugeeRenderer;
import com.teammoeg.frostedheart.foundation.world.entities.CuriosityEntityModel;
import com.teammoeg.frostedheart.foundation.world.entities.CuriosityEntityRenderer;
import net.minecraftforge.client.event.*;

import com.teammoeg.frostedheart.foundation.model.DynamicBlockModelReference;
import com.teammoeg.frostedheart.foundation.model.LiningModel;
import com.teammoeg.frostedheart.foundation.particles.BreathParticle;
import com.teammoeg.frostedheart.foundation.particles.SteamParticle;
import com.teammoeg.frostedheart.compat.tetra.TetraClient;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorScreen;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorRenderer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorRenderer;
import com.teammoeg.frostedheart.content.decoration.RelicChestScreen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.research.blocks.MechCalcRenderer;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatScreen;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaScreen;
import com.teammoeg.frostedheart.content.trade.gui.TradeScreen;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestExtension;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestModel;
import com.teammoeg.frostedheart.util.RegistryUtils;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.client.manual.ManualElementMultiblock;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualEntry.SpecialElementData;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.Tree;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.AddLayers;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FHClientEventsMod {

    private static Tree.InnerNode<ResourceLocation, ManualEntry> CATEGORY;

	@SubscribeEvent
	public static void onCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
		CreativeTabItemHelper helper = new CreativeTabItemHelper(event.getTabKey(), event.getTab());
		ForgeRegistries.ITEMS.forEach(e -> {
			if (e instanceof ICreativeModeTabItem item) {
				item.fillItemCategory(helper);
			}
		});
		helper.register(event);

	}

    public static void addManual() {
        ManualInstance man = ManualHelper.getManual();
        CATEGORY = man.getRoot().getOrCreateSubnode(new ResourceLocation(FHMain.MODID, "main"), 110);
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);

            builder.addSpecialElement(new SpecialElementData("generator", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.Multiblock.GENERATOR_T1)));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator"));
            man.addEntry(CATEGORY, builder.create(), 0);
        }
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
            builder.addSpecialElement(new SpecialElementData("generator_2", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.Multiblock.GENERATOR_T2)));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator_t2"));
            man.addEntry(CATEGORY, builder.create(), 1);
        }
    }

    public static <C extends AbstractContainerMenu, S extends BaseScreen> MenuScreens.ScreenConstructor<C, MenuScreenWrapper<C>>

    FTBScreenFactory(Function<C, S> factory) {
        return (c, i, t) -> new MenuScreenWrapper<>(factory.apply(c), c, i, t).disableSlotDrawing();
    }

    @SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent ev) {
        FHKeyMappings.key_skipDialog.get().setKeyConflictContext(KeyConflictContext.IN_GAME);
        FHKeyMappings.key_InfraredView.get().setKeyConflictContext(KeyConflictContext.IN_GAME);
		ev.register(FHKeyMappings.key_skipDialog.get());
        ev.register(FHKeyMappings.key_InfraredView.get());
	}

	@SubscribeEvent
	public static void onLayerRegister(final RegisterLayerDefinitions event) {
		event.registerLayerDefinition(HeaterVestModel.HEATER_VEST_LAYER, () -> HeaterVestModel.createLayer());
	}

	@SubscribeEvent
	public static void onLayerAdd(final AddLayers event) {
		HeaterVestExtension.MODEL=new HeaterVestModel(Minecraft.getInstance().getEntityModels().bakeLayer(HeaterVestModel.HEATER_VEST_LAYER));
	}

	@SubscribeEvent
	public static void registerModels(ModelEvent.RegisterAdditional ev)
	{
		DynamicBlockModelReference.registeredModels.forEach(rl->{
			ev.register(new ResourceLocation(rl.getNamespace(),rl.getPath().substring(0,rl.getPath().lastIndexOf(".")).substring(7)));
		});
	}
	@SubscribeEvent
	public static void registerBERenders(RegisterRenderers event){
        event.registerBlockEntityRenderer(FHMultiblocks.Logic.GENERATOR_T1.masterBE().get(), T1GeneratorRenderer::new);
        event.registerBlockEntityRenderer(FHMultiblocks.Logic.GENERATOR_T2.masterBE().get(), T2GeneratorRenderer::new);
        event.registerBlockEntityRenderer(FHBlockEntityTypes.MECH_CALC.get(), MechCalcRenderer::new);
	}

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Register screens
    	MenuScreens.register(FHMenuTypes.GENERATOR_T1.getType(), GeneratorScreen<T1GeneratorState, T1GeneratorLogic>::new);
    	MenuScreens.register(FHMenuTypes.GENERATOR_T2.getType(), GeneratorScreen<T2GeneratorState, T2GeneratorLogic>::new);
    	MenuScreens.register(FHMenuTypes.RELIC_CHEST.get(), RelicChestScreen::new);
    	registerFTBScreen(FHMenuTypes.DRAW_DESK.get(), DrawDeskScreen::new);
        registerFTBScreen(FHMenuTypes.TRADE_GUI.get(), TradeScreen::new);
        registerFTBScreen(FHMenuTypes.HEAT_STAT.get(), HeatStatScreen::new);
        MenuScreens.register(FHMenuTypes.SAUNA.get(), SaunaScreen::new);
        MenuScreens.register(FHMenuTypes.INCUBATOR_T1.get(), IncubatorT1Screen::new);
        MenuScreens.register(FHMenuTypes.INCUBATOR_T2.get(), IncubatorT2Screen::new);

        // Register translucent render type
        //TODO: specify in model files
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.RYE_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.WHITE_TURNIP_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.WOLFBERRY_BUSH_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.DRAWING_DESK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.CHARGER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.MECHANICAL_CALCULATOR.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.STEAM_CORE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.DEBUG_HEATER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.RELIC_CHEST.get(), RenderType.cutout());
//        ItemBlockRenderTypes.setRenderLayer(FHBlocks.fluorite_ore.get(), RenderType.cutout());
//        ItemBlockRenderTypes.setRenderLayer(FHBlocks.halite_ore.get(), RenderType.cutout());
/*
        RenderTypeLookup.setRenderLayer(FHBlocks.blood_block, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.bone_block, RenderType.getCutout());
        //RenderTypeLookup.setRenderLayer(FHBlocks.desk, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.small_garage, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.package_block, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.pebble_block, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.odd_mark, RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(FHBlocks.wooden_box, RenderType.getCutout());*/

        
        // Register layers

        addManual();
        if (ModList.get().isLoaded("tetra"))
            TetraClient.init();
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        for (ResourceLocation location : event.getModels().keySet()) {
            // Now find all armors
            ResourceLocation item = new ResourceLocation(location.getNamespace(), location.getPath());
            if (RegistryUtils.getItem(item) instanceof ArmorItem) {
                ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation(item, "inventory");
                BakedModel model = event.getModels().get(itemModelResourceLocation);
                if (model == null) {
                    FHMain.LOGGER.warn("Did not find the expected vanilla baked model for " + item + " in registry");
                } else if (model instanceof LiningModel) {
                	FHMain.LOGGER.warn("Tried to replace " + item + " twice");
                } else {
                    // Replace the model with our IBakedModel
                    LiningModel customModel = new LiningModel(model);
                    event.getModels().put(itemModelResourceLocation, customModel);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTextureStitchEvent(TextureStitchEvent event) {
       /* if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
        	
        	SpriteLoader.create(event.getAtlas()).loadAndStitch(null, null, 0, null)
            event.getAtlas().(LiningFinalizedModel.buffCoatFeetTexture);
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
        }*/
    }
/*
    @SubscribeEvent
    public static void provideTextures(final TextureStitchEvent.Pre event) {
        if (InventoryMenu.BLOCK_ATLAS.equals(event.getMap().location())) {
            Minecraft.getInstance().getResourceManager().listResources("textures/item/module", s -> s.endsWith(".png")).stream()
                    .filter(resourceLocation -> FHMain.MODID.equals(resourceLocation.getNamespace()))
                    // 9 is the length of "textures/" & 4 is the length of ".png"

                    .map(rl -> new ResourceLocation(rl.getNamespace(), rl.getPath().substring(9, rl.getPath().length() - 4)))
                    .peek(rl -> FHMain.LOGGER.info("stitching texture" + rl))
                    .forEach(event::addSprite);
        }
    }
*/
    public static <C extends AbstractContainerMenu, S extends BaseScreen> void
    registerFTBScreen(MenuType<C> type, Function<C, S> factory) {
        MenuScreens.register(type, FTBScreenFactory(factory));
    }

    /*public static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> void
    registerIEScreen(ResourceLocation containerName, MenuScreens.ScreenConstructor<C, S> factory) {
        @SuppressWarnings("unchecked")
        MenuType<C> type = (MenuType<C>) GuiHandler.getContainerType(containerName);
        MenuScreens.register(type, factory);
    }*/

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    	event.registerSpriteSet(FHParticleTypes.STEAM.get(), SteamParticle.Factory::new);
    	event.registerSpriteSet(FHParticleTypes.BREATH.get(), BreathParticle.Factory::new);
    	event.registerSpriteSet(FHParticleTypes.WET_STEAM.get(), WetSteamParticle.Factory::new);
        event.registerSpriteSet(FHParticleTypes.SNOW.get(), SnowParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FHEntityTypes.CURIOSITY.get(), CuriosityEntityRenderer::new);
        event.registerEntityRenderer(FHEntityTypes.WANDERING_REFUGEE.get(), WanderingRefugeeRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CuriosityEntityModel.LAYER_LOCATION, CuriosityEntityModel::createBodyLayer);
    }

}
