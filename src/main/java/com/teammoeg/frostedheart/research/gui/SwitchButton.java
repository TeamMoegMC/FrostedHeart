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

package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.CursorType;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class SwitchButton extends Button {
    boolean state;
    Icon state0,state1;



	public SwitchButton(Panel panel, Icon state0, Icon state1, boolean state) {
		super(panel);
		this.state = state;
		this.state0 = state0;
		this.state1 = state1;
	}

	@Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        if(state)
        	state1.draw(matrixStack, x, y, w, h);
        else
        	state0.draw(matrixStack, x, y, w, h);
    }

	public boolean getState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	@Override
	public void onClicked(MouseButton arg0) {
		state=!state;
		onSwitched();
	}
	public void onSwitched() {
		
	} 


}
