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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.util.utility.OptionalLazy;

import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

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
	public void save(CompoundTag nbt, boolean isPacket) {

		super.save(nbt, isPacket);
        if (ownerName != null)
            nbt.putString("owner", ownerName);
        nbt.putUUID("uuid", id);
        team.ifPresent(t->nbt.putUUID("teamId", t.getId()));//ftb team id
	}
	
	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
		super.load(nbt, isPacket);
		//Compatible migration from old data folder
		if(nbt.contains("researches")) {
			this.setData(SpecialDataTypes.RESEARCH_DATA, SpecialDataTypes.RESEARCH_DATA.loadData(NbtOps.INSTANCE, nbt));
		}
        if (nbt.contains("owner"))
            ownerName = nbt.getString("owner");
        if (nbt.contains("uuid"))
            id = nbt.getUUID("uuid");
        //no need to deserialize ftb team
	}
	
	/**
	 * For each online player.
	 *
	 * @param consumer the player consumer
	 */
	public void forEachOnline(Consumer<ServerPlayer> consumer) {
		if(team.isPresent())
	        for (ServerPlayer spe : team.get().getOnlineMembers())
	        	consumer.accept(spe);
	}
	
	/**
	 * Send packet to all online player.
	 *
	 * @param packet the packet
	 */
	public void sendToOnline(FHMessage packet) {
		if(team.isPresent())
	        for (ServerPlayer spe : team.get().getOnlineMembers())
	        	FHNetwork.sendPlayer(spe, packet);
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
	public Collection<ServerPlayer> getOnlineMembers() {
		return team.get().getOnlineMembers();
	}
	Map<SpecialDataType,TeamDataClosure> dataHolderCache=new HashMap<>();
	public synchronized <U extends SpecialData> TeamDataClosure<U> getDataHolder(SpecialDataType<U> cap){
		return dataHolderCache.computeIfAbsent(cap, t->new TeamDataClosure<>(this,t));
	}

}
