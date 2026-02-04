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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

public class HuntingBaseState extends WorkerState {
	// get max resident
	@Getter
	int maxResidents;
	public HuntingBaseState() {
	}

	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		super.writeNBT(tag, isNetwork);
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		super.readNBT(tag, isNetwork);
	}

}
