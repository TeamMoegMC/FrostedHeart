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
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import net.minecraft.client.gui.GuiGraphics;

public abstract class ImageButton extends UIElement {
	protected CIcon normal,over,pressed;
	protected boolean isPressed;


	public ImageButton(UIElement parent, CIcon normal, CIcon over, CIcon pressed) {
		super(parent);
		this.normal = normal;
		this.over = over;
		this.pressed = pressed;
	}
	public ImageButton(UIElement parent, CIcon normal, CIcon over) {
		this(parent,normal,over,over);
	}
	public ImageButton(UIElement parent, CIcon normal) {
		this(parent,normal,normal);
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		CGuiHelper.resetGuiDrawing();
		if(isMouseOver()) {
			if(isPressed) {
				pressed.draw(graphics, x, y, w, h);
			}else {
				over.draw(graphics, x, y, w, h);
			}
		}else
			normal.draw(graphics, x, y, w, h);
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (isEnabled()) {
				onClicked(button);
				isPressed=true;
			}

			return true;
		}

		return false;
	}

	@Override
	public void onMouseReleased(MouseButton button) {
		super.onMouseReleased(button);
		isPressed=false;
	}
	public abstract void onClicked(MouseButton button);

	@Override
	public Cursor getCursor() {
		return Cursor.HAND;
	}
}