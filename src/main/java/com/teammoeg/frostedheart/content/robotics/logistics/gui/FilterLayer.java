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

import com.teammoeg.chorda.client.cui.UILayer;

import net.minecraft.client.gui.GuiGraphics;

public class FilterLayer extends UILayer {
	RequesterChestScreen layer;
	public FilterLayer(RequesterChestScreen panel) {
		super(panel);
		layer=panel;
	}

	@Override
	public void addUIElements() {
		for(int i=0;i<layer.getMenu().list.size();i++) {
			UIFilterSlot slot=new UIFilterSlot(this,layer.getMenu(),i);
			slot.setPosAndSize(8+18*i, 3, 16, 16);
			add(slot);
			
		}
		setSize(176,31);
	}

	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		LogisticIcons.INV_FILTER.draw(graphics, x, y, w, h);
	}

	@Override
	public void alignWidgets() {

		
	}

}
