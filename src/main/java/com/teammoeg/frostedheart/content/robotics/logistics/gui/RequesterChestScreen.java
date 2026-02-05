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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.CInputHelper;

import net.minecraft.client.gui.GuiGraphics;

public class RequesterChestScreen extends LogisticChestLayer<RequesterChestMenu>{
	FilterLayer filters;
	DockFilterDialog screen;
	public RequesterChestScreen(RequesterChestMenu menu) {
		super(menu);
		filters=new FilterLayer(this);
		filters.setPos(0, 84);
	}

	@Override
	public boolean onInit() {
		
		return super.onInit();
	}

	@Override
	public void addUIElements() {
		if(screen==null) {
			super.addUIElements();
			this.add(filters);
		}else {
			this.add(screen);
		}
		
		
	}
	public void closeFilterLayer() {
		screen.updateFilterSetting();
		screen=null;
		menu.setSlotVisible(true);
		refresh();
	}
	public void openFilterLayer(int index) {
		screen=new DockFilterDialog(this, menu, index);
		menu.setSlotVisible(false);
		refresh();
	}


	@Override
	public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
		if(screen!=null) {
			if (CInputHelper.shouldCloseMenu(keyCode, scanCode)) {
				closeFilterLayer();
				return true;
			}
		}
		return super.onKeyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void alignWidgets() {
		if(screen!=null) {
			this.setSizeToContentSize();
		}else {
			this.setSize(176, 200);
		}
	}

	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		if(screen==null) {
			LogisticIcons.INV_CHEST.draw(graphics, x+0, y+25, 176, 59);
			LogisticIcons.INV_BACK.draw(graphics, x+0, y+115, 176, 84);
			super.drawBackground(graphics, x, y, w, h);
		}
	}

}
