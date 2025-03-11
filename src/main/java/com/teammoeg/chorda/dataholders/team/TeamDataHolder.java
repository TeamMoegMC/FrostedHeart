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
package com.teammoeg.chorda.dataholders.team;

import com.teammoeg.chorda.dataholders.DataHolderMap;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.chorda.network.CBaseNetwork;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.util.struct.OptionalLazy;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Consumer;

/**
 * Data holder for team
 */
public class TeamDataHolder extends DataHolderMap<TeamDataHolder> {
    
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
		super("TeamData");
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
	public void sendToOnline(CBaseNetwork network,CMessage packet) {
		if(team.isPresent())
	        for (ServerPlayer spe : team.get().getOnlineMembers())
	        	network.sendPlayer(spe, packet);
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
