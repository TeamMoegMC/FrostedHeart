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

import com.teammoeg.chorda.client.ClientUtils;

/**
 * CUI调试辅助工具类，控制CUI框架的调试模式开关。
 * 调试模式仅在原版F3调试界面开启时可用，启用后会在UI元素周围显示边框等调试信息。
 * <p>
 * CUI debug helper utility controlling the debug mode toggle for the CUI framework.
 * Debug mode is only available when the vanilla F3 debug screen is enabled;
 * when active, it displays debug information such as borders around UI elements.
 */
public class CUIDebugHelper {
	private static boolean isDebugEnabled;
	private CUIDebugHelper() {

	}
	public static boolean isDebugEnabled() {
		return ClientUtils.getMc().options.renderDebug&&isDebugEnabled;
		
	}
	public static void toggleDebug() {
		if(ClientUtils.getMc().options.renderDebug)
			isDebugEnabled=!isDebugEnabled;
	}

}
