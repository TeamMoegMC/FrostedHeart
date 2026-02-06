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

package com.teammoeg.frostedresearch;

import java.util.function.Function;

import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.client.cui.CUIMenuScreenWrapper;
import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.model.DynamicBlockModelReference;
import com.teammoeg.frostedresearch.blocks.MechCalcRenderer;
import com.teammoeg.frostedresearch.compat.ftb.FTBQCompat;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FRClient {

	public FRClient() {

	}

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent event) {
		MechCalcRenderer.MODEL = DynamicBlockModelReference.getModelCached(FRMain.MODID, "block/mechanical_calculator_movable").register();
		if (CompatModule.isFTBQLoaded())
			FTBQCompat.setRewardGuiProviders();
	}

	@SubscribeEvent
	public static void initScreen(FMLClientSetupEvent event) {
		registerCUIScreen(FRContents.MenuTypes.DRAW_DESK.get(), DrawDeskScreen::new);
	}

	public static <C extends AbstractContainerMenu, S extends PrimaryLayer> void registerCUIScreen(MenuType<C> type, Function<C, S> factory) {
		MenuScreens.register(type, CUIScreenFactory(factory));
	}

	public static <C extends AbstractContainerMenu, S extends PrimaryLayer> MenuScreens.ScreenConstructor<C, CUIMenuScreenWrapper<C>> CUIScreenFactory(Function<C, S> factory) {
		return (c, i, t) -> new CUIMenuScreenWrapper<>(factory.apply(c), c, i, t);
	}
}
