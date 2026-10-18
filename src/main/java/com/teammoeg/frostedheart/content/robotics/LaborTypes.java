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

package com.teammoeg.frostedheart.content.robotics;

import com.teammoeg.frostedheart.content.robotics.labor.LaborType;

/**
 * 已注册的劳工类型。劳工点数按类型分别计算，每种类型有一个独立的池。
 */
public class LaborTypes {

	/** 通用劳工点数。 */
	public static final LaborType WORKER = LaborType.register("worker");

	public static void init() {
	}
}
