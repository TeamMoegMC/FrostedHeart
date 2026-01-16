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

import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import net.minecraft.client.gui.GuiGraphics;

public class CheckBox extends UIElement {
	protected boolean checked;
    protected CIcon uncheckedIcon, checkedIcon;


    public CheckBox(UIElement panel, CIcon uncheckedIcon, CIcon checkedIcon, boolean checked) {
        super(panel);
        this.checked = checked;
        this.uncheckedIcon = uncheckedIcon;
        this.checkedIcon = checkedIcon;
    }

    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        if (checked)
        	checkedIcon.draw(graphics, x, y, w, h);
        else
        	uncheckedIcon.draw(graphics, x, y, w, h);
	}


    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
    	boolean isSwitched=false;
    	if(checked!=this.checked)
    		isSwitched=true;
    	this.checked = checked;
    	if(isSwitched)
    		onSwitched();
    }

    @Override
	public boolean onMousePressed(MouseButton button) {
    	if(!isMouseOver())return false;
    	if(!isEnabled())return false;
    	checked = !checked;
        onSwitched();
		return true;
	}



    public void onSwitched() {

    }


}
