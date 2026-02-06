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

package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class MineBaseState extends WorkerState {
	ResourceLocation biomePath;
	public MineBaseState() {
		
	}

	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		super.writeNBT(tag, isNetwork);
		if(biomePath!=null)
		tag.putString("biome",biomePath.toString());
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		super.readNBT(tag, isNetwork);
		if(tag.contains("biome"))
			biomePath=new ResourceLocation(tag.getString("biome"));
	}

}
