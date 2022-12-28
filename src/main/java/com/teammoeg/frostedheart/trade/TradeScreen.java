/*
 * Copyright (c) 2022 TeamMoeg
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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.gui.ResearchGui;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.inventory.container.Slot;

public class TradeScreen extends BaseScreen implements ResearchGui {

	public TradeScreen() {
		super();

	}

	@Override
	public boolean onInit() {
		int sw = 250;
		int sh = 177;
		this.setSize(sw, sh);
		return super.onInit();
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawPanelBackground(matrixStack, x, y, w, h);
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				theme.drawContainerSlot(matrixStack, 7 + x + i * 16, 15 + y + j * 16, 16, 16);
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				theme.drawContainerSlot(matrixStack, 16 * 5 + 7 + x + i * 16, 15 + y + j * 16, 16, 16);
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				theme.drawContainerSlot(matrixStack, 16 * 9 + 10 + x + i * 16, 15 + y + j * 16, 16, 16);
		theme.drawButton(matrixStack, x+160, y+81,70, 20,WidgetType.NORMAL);
		for (int k = 0; k < 3; ++k) {
			for (int i1 = 0; i1 < 9; ++i1) {
				theme.drawContainerSlot(matrixStack, 8 + i1 * 18, 84 + k * 18, 16, 16);
			}
		}

		for (int l = 0; l < 9; ++l) {
			theme.drawContainerSlot(matrixStack, 8 + l * 18, 142, 16, 16);
		}
	}

	@Override
	public void addWidgets() {
		SimpleTextButton trade;
		super.add(trade=new SimpleTextButton(this,GuiUtils.str("Trade"),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
			}
			
		});
	}

}
