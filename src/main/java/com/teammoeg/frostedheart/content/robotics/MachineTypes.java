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

import com.teammoeg.frostedheart.content.robotics.labor.MachineType;
import com.teammoeg.frostedheart.content.robotics.labor.MachineType.BaseMachineType;

public class MachineTypes {

	/** 物流核心：使用通用劳工，最大等级 10，每级 5 点。 */
	public static final MachineType LOGISTIC = MachineType.register(
			new BaseMachineType(LaborTypes.WORKER, 10, 5), "logistic");

	public static void init() {
		LaborTypes.init();
	}
}
