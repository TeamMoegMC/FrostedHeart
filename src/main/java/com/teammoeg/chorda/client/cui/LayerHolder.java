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

package com.teammoeg.chorda.client.cui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
/**
 * Modifies vanilla color palette
 * Interface method for minecraft screen
 * */
public interface LayerHolder {
	void focusOn(UIElement elm);
	Font getFont();
	void refreshElements();
	/**
	 * @return if the GUI should render a blur effect behind it
	 */
	boolean shouldRenderGradient();
	boolean onCloseQuery();
	Screen getPrevScreen();
	boolean isPauseScreen();
	void closeGui(boolean openPrevScreen);
	void updateGui(double mx, double my, float pt);
}
