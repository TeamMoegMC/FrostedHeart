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

package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.client.manual.ManualElementMultiblock;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.Tree;
import com.teammoeg.frostedheart.compat.ftbq.FHGuiProviders;
import com.teammoeg.frostedheart.compat.tetra.TetraClient;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorScreen;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorState;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.decoration.RelicChestScreen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.KGlyphProvider;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatScreen;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaScreen;
import com.teammoeg.frostedheart.content.trade.gui.TradeScreen;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Function;

import static com.teammoeg.frostedheart.FHMain.*;

public class FHClient {
    public FHClient() {

    }

    private static Tree.InnerNode<ResourceLocation, ManualEntry> CATEGORY;

    public static void init() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;

        LOGGER.info(CLIENT_INIT, "Initializing client");
        FHDynamicModels.setup();
        KGlyphProvider.addListener();

        LOGGER.info(CLIENT_INIT, "Registering client forge event listeners");
        // example: forge.addListener(ModuleClientEventHandler::render);

        LOGGER.info(CLIENT_INIT, "Registering client mod event listeners");
        mod.addListener(FHClient::setup);
    }

    public static void setup(FMLClientSetupEvent event) {
        LOGGER.info(CLIENT_SETUP, "Setting up client");
        FHGuiProviders.setRewardGuiProviders();
        FHKeyMappings.init();
        
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

    public static void addManual() {
        ManualInstance man = ManualHelper.getManual();
        CATEGORY = man.getRoot().getOrCreateSubnode(new ResourceLocation(FHMain.MODID, "main"), 110);
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);

            builder.addSpecialElement(new ManualEntry.SpecialElementData("generator", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.Multiblock.GENERATOR_T1)));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator"));
            man.addEntry(CATEGORY, builder.create(), 0);
        }
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
            builder.addSpecialElement(new ManualEntry.SpecialElementData("generator_2", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.Multiblock.GENERATOR_T2)));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator_t2"));
            man.addEntry(CATEGORY, builder.create(), 1);
        }
    }

    public static <C extends AbstractContainerMenu, S extends BaseScreen> void
    registerFTBScreen(MenuType<C> type, Function<C, S> factory) {
        MenuScreens.register(type, FTBScreenFactory(factory));
    }

    public static <C extends AbstractContainerMenu, S extends BaseScreen> MenuScreens.ScreenConstructor<C, MenuScreenWrapper<C>>
    FTBScreenFactory(Function<C, S> factory) {
        return (c, i, t) -> new MenuScreenWrapper<>(factory.apply(c), c, i, t).disableSlotDrawing();
    }
}
