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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import net.minecraft.client.gui.GuiGraphics;

public class StorageChestScreen extends LogisticChestLayer<StorageChestMenu> {


	public StorageChestScreen(StorageChestMenu container) {
		super(container);
	}
	@Override
	public boolean onInit() {
		this.setSize(176, 169);
		return super.onInit();
	}
	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		LogisticIcons.INV_CHEST.draw(graphics, x+0, y+25, 176, 59);
		LogisticIcons.INV_BACK.draw(graphics, x+0, y+84, 176, 84);
		super.drawBackground(graphics, x, y, w, h);
	}
}
