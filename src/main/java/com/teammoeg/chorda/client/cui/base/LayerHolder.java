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

package com.teammoeg.chorda.client.cui.base;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
/**
 * 层持有者接口，定义CUI层与Minecraft Screen之间的交互契约。
 * 提供焦点管理、字体获取、界面刷新、渐变背景控制、关闭查询、
 * 前一屏幕获取和GUI更新等功能。
 * <p>
 * Layer holder interface defining the interaction contract between CUI layers and
 * Minecraft Screen. Provides focus management, font access, element refresh,
 * gradient background control, close query, previous screen access, and GUI update.
 */
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
	void updateGui(int offX,int offY,double mx, double my, float pt);
}
