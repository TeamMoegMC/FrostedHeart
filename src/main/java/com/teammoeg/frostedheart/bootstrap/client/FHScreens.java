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

package com.teammoeg.frostedheart.bootstrap.client;

import java.util.function.Function;

import com.teammoeg.chorda.client.cui.CUIMenuScreenWrapper;
import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.ui.ScreenAcceptor;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.block.ClothesScreen;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorScreen;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.wardrobe.WardrobeScreen;
import com.teammoeg.frostedheart.content.decoration.RelicChestScreen;
import com.teammoeg.frostedheart.content.health.screen.HealthStatScreen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.robotics.logistics.gui.LogisticChestScreen;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatScreen;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaScreen;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseScreen;
import com.teammoeg.frostedheart.content.trade.gui.TradeScreen;

import com.teammoeg.frostedheart.item.snowsack.SnowSackScreen;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class FHScreens {
    public static void init() {
        // Register screens
        MenuScreens.register(FHMenuTypes.GENERATOR_T1.getType(), GeneratorScreen<T1GeneratorState, T1GeneratorLogic>::new);
        MenuScreens.register(FHMenuTypes.GENERATOR_T2.getType(), GeneratorScreen<T2GeneratorState, T2GeneratorLogic>::new);
        MenuScreens.register(FHMenuTypes.RELIC_CHEST.get(), RelicChestScreen::new);
        
        registerCUIScreen(FHMenuTypes.TRADE_GUI.get(), TradeScreen::new);
        registerCUIScreen(FHMenuTypes.HEAT_STAT.get(), HeatStatScreen::new);
        MenuScreens.register(FHMenuTypes.SAUNA.get(), SaunaScreen::new);
        MenuScreens.register(FHMenuTypes.INCUBATOR_T1.get(), IncubatorT1Screen::new);
        MenuScreens.register(FHMenuTypes.INCUBATOR_T2.get(), IncubatorT2Screen::new);
        MenuScreens.register(FHMenuTypes.CLOTHES_GUI.get(), ClothesScreen::new);
        MenuScreens.register(FHMenuTypes.NUTRITION_GUI.get(), HealthStatScreen::new);
        MenuScreens.register(FHMenuTypes.WARDROBE.get(), WardrobeScreen::new);
        MenuScreens.register(FHMenuTypes.SUPPLY_CHEST.get(), LogisticChestScreen::new);
        MenuScreens.register(FHMenuTypes.STORAGE_CHEST.get(), LogisticChestScreen::new);
        MenuScreens.register(FHMenuTypes.REQUEST_CHEST.get(), LogisticChestScreen::new);
        MenuScreens.register(FHMenuTypes.WAREHOUSE.get(), WarehouseScreen::new);
        MenuScreens.register(FHMenuTypes.SNOW_SACK.get(), SnowSackScreen::new);
    }

    public static <C extends AbstractContainerMenu, S extends BaseScreen> void
    registerFTBScreen(MenuType<C> type, Function<C, S> factory) {
        MenuScreens.register(type, FTBScreenFactory(factory));
    }
    public static <C extends AbstractContainerMenu, S extends PrimaryLayer> void
    registerCUIScreen(MenuType<C> type, Function<C, S> factory) {
        MenuScreens.register(type, CUIScreenFactory(factory));
    }
    public static <C extends AbstractContainerMenu, S extends BaseScreen> MenuScreens.ScreenConstructor<C, MenuScreenWrapper<C>>
    FTBScreenFactory(Function<C, S> factory) {
        return (c, i, t) ->{
        	S menu=factory.apply(c);
        	MenuScreenWrapper<C> msw=new MenuScreenWrapper<>(menu, c, i, t).disableSlotDrawing();
        	if(menu instanceof ScreenAcceptor sa) {
        		sa.setScreen(msw);
        	}
        	return msw;
        };
    }
    public static <C extends AbstractContainerMenu, S extends PrimaryLayer> MenuScreens.ScreenConstructor<C, CUIMenuScreenWrapper<C>>
    CUIScreenFactory(Function<C, S> factory) {
        return (c, i, t) -> new CUIMenuScreenWrapper<>(factory.apply(c), c, i, t);
    }
}
