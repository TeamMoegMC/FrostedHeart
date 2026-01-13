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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import lombok.Getter;
import lombok.Setter;

public abstract class TristateCheckBox extends UIElement {
    boolean enabled;
    CIcon normal, over, locked;
    @Getter
    @Setter
    Consumer<Consumer<Component>> tooltips;

    public TristateCheckBox(UIElement panel, CIcon normal, CIcon over, CIcon locked) {
        super(panel);
        this.normal = normal;
        this.over = over;
        this.locked = locked;
    }

    @Override
	public void getTooltip(Consumer<Component> tooltip) {
		super.getTooltip(tooltip);
		if (tooltips != null)
            tooltips.accept(tooltip);
	}


    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        if (getEnabled()) {
            if (super.isMouseOver())
                over.draw(graphics, x, y, w, h);
            else
                normal.draw(graphics, x, y, w, h);
        } else
            locked.draw(graphics, x, y, w, h);
	}


    @Override
	public Cursor getCursor() {
        if (enabled)
            return Cursor.HAND;
		return super.getCursor();
	}


    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CIcon getLocked() {
        return locked;
    }

    public void setLocked(CIcon locked) {
        this.locked = locked;
    }

    public CIcon getNormal() {
        return normal;
    }

    public void setNormal(CIcon normal) {
        this.normal = normal;
    }

    public CIcon getOver() {
        return over;
    }

    public void setOver(CIcon over) {
        this.over = over;
    }


    public void resetTooltips() {
        this.tooltips = null;
    }

	@Override
	public boolean onMousePressed(MouseButton button) {
		if(!isMouseOver())return false;
		
		return onClicked(button);
	}
	public boolean onClicked(MouseButton button) {
		return false;
	}
}
