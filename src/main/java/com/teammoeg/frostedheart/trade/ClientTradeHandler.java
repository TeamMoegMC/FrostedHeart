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

package com.teammoeg.frostedheart.trade;

import com.teammoeg.frostedheart.trade.gui.TradeScreen;

import blusunrize.immersiveengineering.client.ClientUtils;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.gui.screen.Screen;

public class ClientTradeHandler {
    public static void updateAll() {
        Screen scr = ClientUtils.mc().currentScreen;
        if (scr instanceof MenuScreenWrapper) {
            BaseScreen scr2 = ((MenuScreenWrapper<?>) scr).getGui();
            if (scr2 instanceof TradeScreen) {
                TradeScreen ts = (TradeScreen) scr2;
                ts.updateOffers();
                ts.updateOrders();
            }
        }
    }

    public static void updateBargain() {
        Screen scr = ClientUtils.mc().currentScreen;
        if (scr instanceof MenuScreenWrapper) {
            BaseScreen scr2 = ((MenuScreenWrapper<?>) scr).getGui();
            if (scr2 instanceof TradeScreen) {
                TradeScreen ts = (TradeScreen) scr2;
                ts.updateTrade();
            }
        }
    }

    public static void updateTrade() {
        Screen scr = ClientUtils.mc().currentScreen;
        if (scr instanceof MenuScreenWrapper) {
            BaseScreen scr2 = ((MenuScreenWrapper<?>) scr).getGui();
            if (scr2 instanceof TradeScreen) {
                TradeScreen ts = (TradeScreen) scr2;
                ts.updateTrade();
            }
        }
    }
}
