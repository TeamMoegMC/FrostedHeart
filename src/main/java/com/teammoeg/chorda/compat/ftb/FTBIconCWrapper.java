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

package com.teammoeg.chorda.compat.ftb;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphics;

public class FTBIconCWrapper extends CIcon {
	private final Icon icon;

	public FTBIconCWrapper(Icon icon) {
		this.icon = icon;
	}

	@Override
	public void draw(GuiGraphics arg0, int arg1, int arg2, int arg3, int arg4) {
		try {
		icon.draw(arg0, arg1, arg2, arg3, arg4);
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public boolean isEmpty() {
		return icon.isEmpty();
	}

}