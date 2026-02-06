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
