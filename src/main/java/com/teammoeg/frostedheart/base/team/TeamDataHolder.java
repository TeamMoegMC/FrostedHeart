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

/*
 * 
 */
package com.teammoeg.frostedheart.base.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.util.utility.OptionalLazy;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * Data holder for team
 */
public class TeamDataHolder extends BaseDataHolder<TeamDataHolder> {
    
    /** The frostedheart team id. */
    private UUID id;
    
    /** The player name of owner. */
    private String ownerName;
    
    /** The FTB team. */
    private OptionalLazy<Team> team;
	
	/**
	 * Instantiates a new team data holder.
	 *
	 * @param id the frostedheart team id
	 * @param team the FTB team
	 */
	public TeamDataHolder(UUID id,OptionalLazy<Team> team) {
		this.team=team;
		this.id=id;
	}
	
	@Override
	public void save(CompoundNBT nbt, boolean isPacket) {

		super.save(nbt, isPacket);
        if (ownerName != null)
            nbt.putString("owner", ownerName);
        nbt.putUniqueId("uuid", id);
        team.ifPresent(t->nbt.putUniqueId("teamId", t.getId()));//ftb team id
	}
	
	@Override
	public void load(CompoundNBT nbt, boolean isPacket) {
		super.load(nbt, isPacket);
		//Compatible migration from old data folder
		if(nbt.contains("researches")) {
			this.getData(SpecialDataTypes.RESEARCH_DATA).deserialize(nbt, isPacket);
		}
        if (nbt.contains("owner"))
            ownerName = nbt.getString("owner");
        if (nbt.contains("uuid"))
            id = nbt.getUniqueId("uuid");
        //no need to deserialize ftb team
	}
	
	/**
	 * For each online player.
	 *
	 * @param consumer the player consumer
	 */
	public void forEachOnline(Consumer<ServerPlayerEntity> consumer) {
        for (ServerPlayerEntity spe : team.get().getOnlineMembers())
        	consumer.accept(spe);
	}
	
	/**
	 * Send packet to all online player.
	 *
	 * @param packet the packet
	 */
	public void sendToOnline(FHMessage packet) {
        for (ServerPlayerEntity spe : team.get().getOnlineMembers())
        	FHNetwork.send(PacketDistributor.PLAYER.with(()->spe), packet);
	}
    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }
    public Optional<Team> getTeam() {
        if (team == null)
            return Optional.empty();
        return team.resolve();
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setTeam(OptionalLazy<Team> team) {
        this.team = team;
    }
	
	/**
	 * Get all online members.
	 *
	 * @return the online members
	 */
	public List<ServerPlayerEntity> getOnlineMembers() {
		return team.get().getOnlineMembers();
	}
}
