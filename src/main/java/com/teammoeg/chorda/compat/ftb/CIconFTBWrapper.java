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

package com.teammoeg.chorda.compat.ftb;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 将Chorda图标（CIcon）包装为FTB Library图标（Icon）的适配器。
 * 允许在FTB Library的图标系统中使用Chorda图标。
 * <p>
 * Adapter that wraps a Chorda icon (CIcon) as an FTB Library icon (Icon).
 * Allows Chorda icons to be used within FTB Library's icon system.
 */
public class CIconFTBWrapper extends Icon {
	/** Chorda图标实例 / The Chorda icon instance */
	private final CIcon icon;

	/**
	 * 创建一个CIcon的FTB包装器。
	 * <p>
	 * Creates an FTB wrapper for a CIcon.
	 *
	 * @param icon 要包装的Chorda图标 / The Chorda icon to wrap
	 */
	public CIconFTBWrapper(CIcon icon) {
		this.icon = icon;
	}

	/**
	 * 绘制被包装的Chorda图标。捕获并打印绘制过程中的异常。
	 * <p>
	 * Draws the wrapped Chorda icon. Catches and prints any exceptions during drawing.
	 *
	 * @param arg0 图形上下文 / The graphics context
	 * @param arg1 X坐标 / The X coordinate
	 * @param arg2 Y坐标 / The Y coordinate
	 * @param arg3 宽度 / The width
	 * @param arg4 高度 / The height
	 */
	@Override
	public void draw(GuiGraphics arg0, int arg1, int arg2, int arg3, int arg4) {
		try {
		icon.draw(arg0, arg1, arg2, arg3, arg4);
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}

}