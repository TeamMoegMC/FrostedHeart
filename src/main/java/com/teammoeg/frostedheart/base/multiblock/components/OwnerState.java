package com.teammoeg.frostedheart.base.multiblock.components;

import java.util.Optional;
import java.util.UUID;

import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;

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

    protected Optional<TeamDataHolder> getTeamData() {
        UUID owner = getOwner();
        if (owner != null)
            return Optional.ofNullable(FHTeamDataManager.getDataByResearchID(owner));
        return Optional.empty();
    }



}
