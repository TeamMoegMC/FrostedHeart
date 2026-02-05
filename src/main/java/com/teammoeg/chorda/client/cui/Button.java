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

package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.text.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;

public abstract class Button extends UIElement {
	protected Component title;
	protected CIcon icon;

	public Button(UIElement panel, Component t, CIcon i) {
		super(panel);
		setSize(16, 16);
		icon = i;
		title = t;
	}

	public Button(UIElement panel) {
		this(panel, Components.immutableEmpty(), CIcons.nop());
	}

	@Override
	public Component getTitle() {
		return title;
	}

	public Button setTitle(Component s) {
		title = s;
		fitSize();
		return this;
	}

	public boolean hasIcon() {
		return icon!=CIcons.nop();
	}
	public Button setIcon(CIcon i) {
		icon = i;
		fitSize();
		return this;
	}
	protected void fitSize() {
		setWidth(parent.getFont().width(title)+((Components.isEmpty(title)&&hasIcon())?0:8) + (hasIcon() ? 20 : 0));
		setHeight(hasIcon() ?20:16);
	}
	private int getTextureY() {
		int i = 1;
		if (!this.isEnabled()) {
			i = 0;
		} else if (this.isMouseOver()) {
			i = 2;
		}

		return 46 + i * 20;
	}

	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, this.getTextureY());

	}

	public void drawIcon(GuiGraphics graphics, int x, int y, int w, int h) {
		icon.draw(graphics, x, y, w, h);
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		CGuiHelper.resetGuiDrawing();
		var s = h >= 16 ? 16 : 8;
		drawBackground(graphics, x, y, w, h);
		drawIcon(graphics, x + (w - s) / 2, y + (h - s) / 2, s, s);
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (isEnabled()) {
				onClicked(button);
			}

			return true;
		}

		return false;
	}

	public abstract void onClicked(MouseButton button);

	@Override
	public Cursor getCursor() {
		return Cursor.HAND;
	}
}