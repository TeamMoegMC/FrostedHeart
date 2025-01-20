/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.chorda.util.client;

import net.minecraft.client.gui.components.events.GuiEventListener;

public class GuiClickedEvent implements GuiEventListener {
    int x1;
    int y1;
    int x2;
    int y2;
    Runnable call;
    boolean focused;
    public GuiClickedEvent(int x1, int y1, int x2, int y2, Runnable call) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.call = call;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (x1 <= mx && mx <= x2 && y1 <= my && my <= y2) {
            call.run();
            return true;
        }
        return false;
    }

	@Override
	public void setFocused(boolean pFocused) {
		focused=pFocused;
	}

	@Override
	public boolean isFocused() {
		return focused;
	}

}
