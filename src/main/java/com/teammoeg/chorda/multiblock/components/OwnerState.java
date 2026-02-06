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

package com.teammoeg.chorda.multiblock.components;

import java.util.Optional;
import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.nbt.CompoundTag;

public class OwnerState implements IMultiblockState, IOwnerState<OwnerState> {
	UUID owner=null;
	public OwnerState() {
	}

	@Override
	public UUID getOwner() {
		return owner;
	}

	@Override
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		if(owner!=null)
			nbt.putUUID("owner", owner);
		
	}

	@Override
	public void readSaveNBT(CompoundTag nbt) {
		if(nbt.contains("owner"))
			owner=nbt.getUUID("owner");
		
	}

    public Optional<TeamDataHolder> getTeamData() {
        UUID owner = getOwner();
        if (owner != null)
            return Optional.ofNullable(CTeamDataManager.getDataByResearchID(owner));
        return Optional.empty();
    }

	@Override
	public void onOwnerChange(IMultiblockContext<? extends OwnerState> ctx) {
		
	}



}
