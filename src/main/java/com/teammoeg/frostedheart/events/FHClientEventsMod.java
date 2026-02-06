/*
 * Copyright (c) 2026 TeamMoeg
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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.*;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.util.ShaderCompatHelper;
import com.teammoeg.frostedheart.compat.ie.FHManual;
import com.teammoeg.frostedheart.compat.tetra.TetraClient;
import com.teammoeg.frostedheart.content.climate.particle.SnowParticle;
import com.teammoeg.frostedheart.content.climate.tooltips.BlockTempStats;
import com.teammoeg.frostedheart.content.climate.tooltips.EquipmentTempStats;
import com.teammoeg.frostedheart.content.climate.tooltips.FoodTempStats;
import com.teammoeg.frostedheart.content.climate.tooltips.PlantTempStats;
import com.teammoeg.frostedheart.content.health.tooltip.FoodNutritionStats;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.KGlyphProvider;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugeeRenderer;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestExtension;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestModel;
import com.teammoeg.frostedheart.content.utility.seld.ContainerHolderEntityRenderer;
import com.teammoeg.frostedheart.content.utility.seld.ContainerHolderModel;
import com.teammoeg.frostedheart.content.utility.seld.SledEntityRenderer;
import com.teammoeg.frostedheart.content.utility.seld.SledModel;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuRenderer;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.bootstrap.client.FHScreens;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHParticleTypes;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorRenderer;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorRenderer;
import com.teammoeg.frostedheart.content.climate.particle.BreathParticle;
import com.teammoeg.frostedheart.content.climate.particle.SteamParticle;
import com.teammoeg.frostedheart.content.climate.particle.WetSteamParticle;
import com.teammoeg.frostedheart.content.world.entities.CuriosityEntityModel;
import com.teammoeg.frostedheart.content.world.entities.CuriosityEntityRenderer;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedheart.util.client.PropertyRegistrationHelper;
import com.teammoeg.frostedresearch.gui.InsightOverlay;

import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.EntityRenderersEvent.AddLayers;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

import static com.teammoeg.frostedheart.FHMain.*;

/**
 * Client side events fired on mod bus.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FHClientEventsMod {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        LOGGER.info(CLIENT_SETUP, "FML Client setup event started");

        LOGGER.info(CLIENT_SETUP, "Initializing Key Mappings");
        FHKeyMappings.init();
        LOGGER.info(CLIENT_SETUP, "Initializing Screens");
        FHScreens.init();

        if (CompatModule.isIELoaded()) {
            LOGGER.info(CLIENT_SETUP, "Initializing IE Manual");
            FHManual.init();
        }
        if (CompatModule.isTetraLoaded()) {
            LOGGER.info(CLIENT_SETUP, "Initializing Tetra Client");
            TetraClient.init();
            
        }
        //if (CompatModule.isFTBQLoaded()) {
        //    LOGGER.info(CLIENT_SETUP, "Initializing FTB Quests");
        //    FHGuiProviders.setRewardGuiProviders();
        //}
        LOGGER.info(CLIENT_SETUP, "FML Client setup event finished");

        REGISTRATE.setTooltipModifierFactory(item -> {
            return new ItemDescription.Modifier(item, TooltipHelper.Palette.STANDARD_CREATE)
                    .andThen(new FoodTempStats(item))
				.andThen(TooltipModifier.mapNull(FoodNutritionStats.create(item)))
				.andThen(TooltipModifier.mapNull(PlantTempStats.create(item)))
				.andThen(TooltipModifier.mapNull(BlockTempStats.create(item)))
				.andThen(TooltipModifier.mapNull(EquipmentTempStats.create(item)))
				.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
		});


        //new PropertyRegistrationHelper(event);

        ShaderCompatHelper.use(Blocks.PACKED_ICE)
        .add(FHBlocks.FIRM_ICE_BLOCK);
        ShaderCompatHelper.use(Blocks.SNOW_BLOCK)
        // twigs and debris
        .add(FHBlocks.BESNOWED_TWIGS_BLOCK)
        .add(FHBlocks.BESNOWED_DEBRIS_BLOCK)
        // condensed ores
        .add(FHBlocks.CONDENSED_IRON_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_COPPER_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_GOLD_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_ZINC_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_SILVER_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_TIN_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_PYRITE_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_NICKEL_ORE_BLOCK)
        .add(FHBlocks.CONDENSED_LEAD_ORE_BLOCK)
        // Sludge
        .add(FHBlocks.IRON_SLUDGE_BLOCK)
        .add(FHBlocks.COPPER_SLUDGE_BLOCK)
        .add(FHBlocks.GOLD_SLUDGE_BLOCK)
        .add(FHBlocks.ZINC_SLUDGE_BLOCK)
        .add(FHBlocks.SILVER_SLUDGE_BLOCK)
        .add(FHBlocks.TIN_SLUDGE_BLOCK)
        .add(FHBlocks.PYRITE_SLUDGE_BLOCK)
        .add(FHBlocks.NICKEL_SLUDGE_BLOCK)
        .add(FHBlocks.LEAD_SLUDGE_BLOCK)
        
        .add(FHBlocks.PACKED_SNOW)
        .add(FHBlocks.PACKED_SNOW_SLAB);
        ShaderCompatHelper.use(Blocks.SNOW)
        .addSameProperty(FHBlocks.LAYERED_THIN_ICE)
        // twigs and debris
        .addSameProperty(FHBlocks.BESNOWED_TWIGS)
        .addSameProperty(FHBlocks.BESNOWED_DEBRIS)
        // condensed ores
        .addSameProperty(FHBlocks.CONDENSED_IRON_ORE)
        .addSameProperty(FHBlocks.CONDENSED_COPPER_ORE)
        .addSameProperty(FHBlocks.CONDENSED_GOLD_ORE)
        .addSameProperty(FHBlocks.CONDENSED_ZINC_ORE)
        .addSameProperty(FHBlocks.CONDENSED_SILVER_ORE)
        .addSameProperty(FHBlocks.CONDENSED_TIN_ORE)
        .addSameProperty(FHBlocks.CONDENSED_PYRITE_ORE)
        .addSameProperty(FHBlocks.CONDENSED_NICKEL_ORE)
        .addSameProperty(FHBlocks.CONDENSED_LEAD_ORE)
        // Sludge
        .addSameProperty(FHBlocks.IRON_SLUDGE)
        .addSameProperty(FHBlocks.COPPER_SLUDGE)
        .addSameProperty(FHBlocks.GOLD_SLUDGE)
        .addSameProperty(FHBlocks.ZINC_SLUDGE)
        .addSameProperty(FHBlocks.SILVER_SLUDGE)
        .addSameProperty(FHBlocks.TIN_SLUDGE)
        .addSameProperty(FHBlocks.PYRITE_SLUDGE)
        .addSameProperty(FHBlocks.NICKEL_SLUDGE)
        .addSameProperty(FHBlocks.LEAD_SLUDGE);
        // Stone ores
        ShaderCompatHelper.use(Blocks.COPPER_ORE)
        .add(FHBlocks.TIN_ORE);
        ShaderCompatHelper.use(Blocks.IRON_ORE)
        .add(FHBlocks.PYRITE_ORE)
        .add(AllBlocks.ZINC_ORE);
        ShaderCompatHelper.use(Blocks.DEEPSLATE_IRON_ORE)
        .add(FHBlocks.HALITE_ORE)
        .add(FHBlocks.SYLVITE_ORE)
        .add(FHBlocks.MAGNESITE_ORE)
        .add(FHBlocks.DEEPSLATE_PYRITE_ORE)
        .add(FHBlocks.DEEPSLATE_HALITE_ORE)
        .add(FHBlocks.DEEPSLATE_SYLVITE_ORE)
        .add(FHBlocks.DEEPSLATE_MAGNESITE_ORE)
        .add(AllBlocks.DEEPSLATE_ZINC_ORE);
        ShaderCompatHelper.use(Blocks.DEEPSLATE_COPPER_ORE)
        .add(FHBlocks.DEEPSLATE_TIN_ORE);
        ShaderCompatHelper.use(Blocks.DEEPSLATE_GOLD_ORE)
        .add(IEBlocks.Metals.DEEPSLATE_ORES.get(EnumMetals.SILVER));
        ShaderCompatHelper.use(Blocks.GOLD_ORE)
        .add(IEBlocks.Metals.ORES.get(EnumMetals.SILVER));
        ShaderCompatHelper.use(Blocks.MUD)
        .add(FHBlocks.PEAT)
        .add(FHBlocks.ROTTEN_WOOD);
        ShaderCompatHelper.use(Blocks.CLAY)
        .add(FHBlocks.BAUXITE)
        .add(FHBlocks.KAOLIN);
        ShaderCompatHelper.use(Blocks.MYCELIUM)
        .add(FHBlocks.BURIED_MYCELIUM);
        ShaderCompatHelper.use(Blocks.PODZOL)
        .add(FHBlocks.BURIED_PODZOL);
        //TODO: find a better category for permafrost
        ShaderCompatHelper.use(Blocks.CLAY)
        .add(FHBlocks.DIRT_PERMAFROST)
        .add(FHBlocks.MUD_PERMAFROST)
        .add(FHBlocks.GRAVEL_PERMAFROST)
        .add(FHBlocks.SAND_PERMAFROST)
        .add(FHBlocks.RED_SAND_PERMAFROST)
        .add(FHBlocks.CLAY_PERMAFROST)
        .add(FHBlocks.PEAT_PERMAFROST)
        .add(FHBlocks.BAUXITE_PERMAFROST)
        .add(FHBlocks.KAOLIN_PERMAFROST)
        .add(FHBlocks.MYCELIUM_PERMAFROST)
        .add(FHBlocks.PODZOL_PERMAFROST)
        .add(FHBlocks.ROOTED_DIRT_PERMAFROST)
        .add(FHBlocks.COARSE_DIRT_PERMAFROST);
        ShaderCompatHelper.use(Blocks.WET_SPONGE)
        .add(FHBlocks.WHALE_BLOCK)
        .add(FHBlocks.WHALE_BELLY_BLOCK);
        ShaderCompatHelper.use(Blocks.GRAVEL)
        .add(FHBlocks.COPPER_GRAVEL);
        ShaderCompatHelper.use(Blocks.WHEAT)
        .add(FHBlocks.RYE_BLOCK)
        .add(FHBlocks.WHITE_TURNIP_BLOCK);
        ShaderCompatHelper.use(Blocks.FERN)
        .add(FHBlocks.RUBBER_DANDELION)
        .add(FHBlocks.WILD_RUBBER_DANDELION);
//        ShaderCompatHelper.use(Blocks.MAGMA_BLOCK)
//        .add(FHBlocks.COOLED_MAGMA_BLOCK);
        ShaderCompatHelper.use(Blocks.FARMLAND)
        .addMapped(FHBlocks.FERTILIZED_FARMLAND, (van,add)->van.setValue(FarmBlock.MOISTURE,add.getValue(FarmBlock.MOISTURE)));
        ShaderCompatHelper.use(Blocks.DIRT)
        .add(FHBlocks.FERTILIZED_DIRT);
        ShaderCompatHelper.use(Blocks.ICE).add(FHBlocks.THIN_ICE_BLOCK);
        ShaderCompatHelper.use(Blocks.IRON_BLOCK)
        .add(FHBlocks.ALUMINUM_BLOCK)
        .add(FHBlocks.STEEL_BLOCK)
        .add(FHBlocks.DURALUMIN_BLOCK)
        .add(FHBlocks.SILVER_BLOCK)
        .add(FHBlocks.NICKEL_BLOCK)
        .add(FHBlocks.TITANIUM_BLOCK)
        .add(FHBlocks.INVAR_BLOCK)
        .add(FHBlocks.TIN_BLOCK)
        .add(FHBlocks.MAGNESIUM_BLOCK);
        ShaderCompatHelper.use(Blocks.GOLD_BLOCK)
        .add(FHBlocks.ELECTRUM_BLOCK);
        ShaderCompatHelper.use(Blocks.COPPER_BLOCK)
        .add(FHBlocks.CONSTANTAN_BLOCK)
        .add(FHBlocks.BRONZE_BLOCK);
        ShaderCompatHelper.use(Blocks.COAL_BLOCK)
        .add(FHBlocks.CAST_IRON_BLOCK)
        .add(FHBlocks.LEAD_BLOCK)
        .add(FHBlocks.TUNGSTEN_STEEL_BLOCK)
        .add(FHBlocks.TUNGSTEN_BLOCK);
        //ruin blocks
        ShaderCompatHelper.use(Blocks.IRON_BLOCK)
        .addAll(FHBlocks.LAB_BLOCK_SCREEN_ON);
        ShaderCompatHelper.use(Blocks.PRISMARINE_BRICKS)
        .add(FHBlocks.LAB_BLOCK)
        .add(FHBlocks.LAB_BLOCK_ACOUSTIC_DIFFUSER)
        .add(FHBlocks.LAB_BLOCK_CABINET)
        .add(FHBlocks.LAB_BLOCK_SCREEN)
        .add(FHBlocks.LAB_BLOCK_SMALL_TILE)
        .add(FHBlocks.LAB_BLOCK_TILE)
        .add(FHBlocks.LAB_VENT)
        .addAll(FHBlocks.LAB_BLOCK_ALPHABET)
        .addAll(FHBlocks.LAB_BLOCK_NUMBER)
        .addAll(FHBlocks.LAB_BLOCK_SIGN)
        .add(FHBlocks.STUDDED_LAB_BLOCK)
        .add(FHBlocks.FRAMED_LAB_BLOCK);
        ShaderCompatHelper.use(Blocks.GRAY_CONCRETE)
        .add(FHBlocks.CONCRETE)
        .addAll(FHBlocks.CONCRETE_CRACKED);
        ShaderCompatHelper.use(Blocks.SEA_LANTERN)
        .add(FHBlocks.LAB_PANEL_LIGHT);
        // FHTooltips.registerTooltipModifiers();
        /*
         ItemBlockRenderTypes.setRenderLayer(FHBlocks.RYE_BLOCK.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(FHBlocks.WHITE_TURNIP_BLOCK.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(FHBlocks.WOLFBERRY_BUSH_BLOCK.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(FHMultiblocks.GENERATOR_T1.getBlock(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(FHMultiblocks.GENERATOR_T2.getBlock(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(FHBlocks.DRAWING_DESK.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(FHBlocks.CHARGER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.MECHANICAL_CALCULATOR.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.STEAM_CORE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHMultiblocks.RADIATOR.getBlock(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.DEBUG_HEATER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(FHBlocks.RELIC_CHEST.get(), RenderType.cutout()); 
         */
    }
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
    	event.registerAboveAll("insight", new InsightOverlay());
        event.registerAboveAll("wheel_menu", WheelMenuRenderer.OVERLAY);
    }
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(KGlyphProvider.INSTANCE);
    }
    

	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent ev) {

		ev.register(FHKeyMappings.key_skipDialog.get());
		if(CompatModule.isLdLibLoaded())
			ev.register(FHKeyMappings.key_InfraredView.get());
        ev.register(FHKeyMappings.key_health.get());
        ev.register(FHKeyMappings.key_clothes.get());
        ev.register(FHKeyMappings.key_openWheelMenu.get());
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
	public static void registerBERenders(RegisterRenderers event){
		FHMain.LOGGER.info("===========Dynamic Block Renderers========");
        event.registerBlockEntityRenderer(FHMultiblocks.Registration.GENERATOR_T1.masterBE().get(), T1GeneratorRenderer::new);
        event.registerBlockEntityRenderer(FHMultiblocks.Registration.GENERATOR_T2.masterBE().get(), T2GeneratorRenderer::new);
	}

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
    	//TODO Remove lining 
        /*for (ResourceLocation location : event.getModels().keySet()) {
            // Now find all armors
            ResourceLocation item = new ResourceLocation(location.getNamespace(), location.getPath());
            if (CRegistryHelper.getItem(item) instanceof ArmorItem) {
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
        }*/
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
        event.registerEntityRenderer(FHEntityTypes.SLED.get(), SledEntityRenderer::new);
        event.registerEntityRenderer(FHEntityTypes.CONTAINER_ENTITY.get(), ContainerHolderEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CuriosityEntityModel.LAYER_LOCATION, CuriosityEntityModel::createBodyLayer);
        event.registerLayerDefinition(SledModel.SLED_LAYER, SledModel::createBodyLayer);
        event.registerLayerDefinition(ContainerHolderModel.CONTAINER_HOLDER, ContainerHolderModel::createBodyLayer);
        event.registerLayerDefinition(SledModel.QUILT_LAYER, SledEntityRenderer.QuiltModel::createBodyLayer);

    }
	@SubscribeEvent
	public static void onTint(RegisterColorHandlersEvent.Item ev) {
		ev.register((a, idx) -> {
			if(idx==1) {
	
			FluidStack stack=a.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(t->t.getFluidInTank(0)).get();
			return stack.isEmpty()?0xff733f31:CGuiHelper.getFluidColor(stack);
				}else return -1;
			}, FHItems.ceramic_bucket.asItem());
		
	}

}
