package com.teammoeg.frostedheart.bootstrap.client;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorScreen;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorState;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.WardrobeScreen;
import com.teammoeg.frostedheart.content.decoration.RelicChestScreen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatScreen;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaScreen;
import com.teammoeg.frostedheart.content.trade.gui.TradeScreen;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Function;

public class FHScreens {
    public static void init() {
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
        MenuScreens.register(FHMenuTypes.WARDROBE.get(), WardrobeScreen::new);
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
