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
import com.teammoeg.chorda.events.TeamLoadedEvent;
import com.teammoeg.chorda.network.CBaseNetwork;
import com.teammoeg.chorda.network.CMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

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
    
    /** The Modded team. */
    private AbstractTeam team;
	
    /** True if this team is correctly loaded*/
    private boolean isLoaded;
	/**
	 * Instantiates a new team data holder.
	 *
	 * @param id the frostedheart team id
	 * @param team the FTB team
	 */
	public TeamDataHolder(UUID id,AbstractTeam team) {
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
        if(team!=null)
        	nbt.putUUID("teamId", team.getId());//team id
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
	public void loadIfNeeded() {
		if(!isLoaded) {
			MinecraftForge.EVENT_BUS.post(new TeamLoadedEvent(this));
			isLoaded=true;
		}
	}
	/**
	 * For each online player.
	 *
	 * @param consumer the player consumer
	 */
	public void forEachOnline(Consumer<ServerPlayer> consumer) {
        for (ServerPlayer spe : team.getOnlineMembers())
        	consumer.accept(spe);
	}
	
	/**
	 * Send packet to all online player.
	 *
	 * @param packet the packet
	 */
	public void sendToOnline(CBaseNetwork network,CMessage packet) {
        for (ServerPlayer spe : team.getOnlineMembers())
        	network.sendPlayer(spe, packet);
	}
    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }
    public AbstractTeam getTeam() {
        return team;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setTeam(AbstractTeam team) {
        this.team = team;
    }
	
	/**
	 * Get all online members.
	 *
	 * @return the online members
	 */
	public Collection<ServerPlayer> getOnlineMembers() {
		return team.getOnlineMembers();
	}
	Map<SpecialDataType,TeamDataClosure> dataHolderCache=new HashMap<>();
	public synchronized <U extends SpecialData> TeamDataClosure<U> getDataHolder(SpecialDataType<U> cap){
		return dataHolderCache.computeIfAbsent(cap, t->new TeamDataClosure<>(this,t));
	}

}
