package com.teammoeg.frostedheart.trade;

import com.teammoeg.frostedheart.trade.gui.TradeScreen;

import blusunrize.immersiveengineering.client.ClientUtils;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.gui.screen.Screen;

public class ClientTradeHandler {
	public static void updateBargain() {
		Screen scr=ClientUtils.mc().currentScreen;
		if(scr instanceof MenuScreenWrapper) {
			BaseScreen scr2=((MenuScreenWrapper<?>) scr).getGui();
			if(scr2 instanceof TradeScreen) {
				TradeScreen ts=(TradeScreen)scr2;
				ts.updateTrade();
			}
		}
	}
	public static void updateAll() {
		Screen scr=ClientUtils.mc().currentScreen;
		if(scr instanceof MenuScreenWrapper) {
			BaseScreen scr2=((MenuScreenWrapper<?>) scr).getGui();
			if(scr2 instanceof TradeScreen) {
				TradeScreen ts=(TradeScreen)scr2;
				ts.updateOffers();
				ts.updateOrders();
			}
		}
	}
	public static void updateTrade() {
		Screen scr=ClientUtils.mc().currentScreen;
		if(scr instanceof MenuScreenWrapper) {
			BaseScreen scr2=((MenuScreenWrapper<?>) scr).getGui();
			if(scr2 instanceof TradeScreen) {
				TradeScreen ts=(TradeScreen)scr2;
				ts.updateTrade();
			}
		}
	}
}
