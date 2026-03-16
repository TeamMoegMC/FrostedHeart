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
 * 将FTB Library图标（Icon）包装为Chorda图标（CIcon）的适配器。
 * 允许在Chorda的图标系统中使用FTB Library图标。
 * <p>
 * Adapter that wraps an FTB Library icon (Icon) as a Chorda icon (CIcon).
 * Allows FTB Library icons to be used within Chorda's icon system.
 */
public class FTBIconCWrapper extends CIcon {
	/** FTB Library图标实例 / The FTB Library icon instance */
	private final Icon icon;

	/**
	 * 创建一个FTB Library图标的Chorda包装器。
	 * <p>
	 * Creates a Chorda wrapper for an FTB Library icon.
	 *
	 * @param icon 要包装的FTB Library图标 / The FTB Library icon to wrap
	 */
	public FTBIconCWrapper(Icon icon) {
		this.icon = icon;
	}

	/**
	 * 绘制被包装的FTB Library图标。捕获并打印绘制过程中的异常。
	 * <p>
	 * Draws the wrapped FTB Library icon. Catches and prints any exceptions during drawing.
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

	/**
	 * 检查被包装的FTB Library图标是否为空。
	 * <p>
	 * Checks whether the wrapped FTB Library icon is empty.
	 *
	 * @return 如果图标为空则为true / true if the icon is empty
	 */
	@Override
	public boolean isEmpty() {
		return icon.isEmpty();
	}

}