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
 * 团队的数据持有者，继承自 {@link DataHolderMap}。
 * 存储团队的所有数据组件，并提供成员管理、网络同步和序列化功能。
 * <p>
 * Data holder for teams, extending {@link DataHolderMap}.
 * Stores all data components for a team and provides member management, network synchronization, and serialization capabilities.
 */
public class TeamDataHolder extends DataHolderMap<TeamDataHolder> {
    
    /** Frosted Heart 团队 ID。 / The Frosted Heart team ID. */
    private UUID id;
    
    /** 所有者的玩家名称。 / The player name of the owner. */
    private String ownerName;
    
    /** 关联的模组团队实例。 / The associated modded team instance. */
    private AbstractTeam team;
	
    /** 标记此团队是否已正确加载。 / Flag indicating whether this team has been correctly loaded. */
    private boolean isLoaded;
	/**
	 * 创建一个新的团队数据持有者。
	 * <p>
	 * Creates a new team data holder.
	 *
	 * @param id Frosted Heart 团队 ID / the Frosted Heart team ID
	 * @param team 关联的团队 / the associated team
	 */
	public TeamDataHolder(UUID id,AbstractTeam team) {
		super("TeamData");
		this.team=team;
		this.id=id;
	}
	
	/**
	 * {@inheritDoc}
	 * 序列化团队数据到 NBT，包括所有者名称、团队 UUID 和团队 ID。
	 * <p>
	 * Serializes team data to NBT, including owner name, team UUID, and team ID.
	 */
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {

		super.save(nbt, isPacket);
        if (ownerName != null)
            nbt.putString("owner", ownerName);
        nbt.putUUID("uuid", id);
        if(team!=null)
        	nbt.putUUID("teamId", team.getId());//team id
	}
	
	/**
	 * {@inheritDoc}
	 * 从 NBT 反序列化团队数据，包括所有者名称和团队 UUID。
	 * <p>
	 * Deserializes team data from NBT, including owner name and team UUID.
	 */
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
	 * 如果尚未加载，则触发加载事件。仅在首次调用时触发 {@link TeamLoadedEvent}。
	 * <p>
	 * Triggers the load event if not yet loaded. Only fires {@link TeamLoadedEvent} on the first call.
	 */
	public void loadIfNeeded() {
		if(!isLoaded) {
			MinecraftForge.EVENT_BUS.post(new TeamLoadedEvent(this));
			isLoaded=true;
		}
	}
	/**
	 * 对每个在线成员执行指定操作。
	 * <p>
	 * Executes the specified action for each online member.
	 *
	 * @param consumer 对每个在线玩家执行的操作 / the action to perform on each online player
	 */
	public void forEachOnline(Consumer<ServerPlayer> consumer) {
        for (ServerPlayer spe : team.getOnlineMembers())
        	consumer.accept(spe);
	}
	
	/**
	 * 向所有在线成员发送网络数据包。
	 * <p>
	 * Sends a network packet to all online members.
	 *
	 * @param network 网络实例 / the network instance
	 * @param packet 要发送的数据包 / the packet to send
	 */
	public void sendToOnline(CBaseNetwork network,CMessage packet) {
        for (ServerPlayer spe : team.getOnlineMembers())
        	network.sendPlayer(spe, packet);
	}
    /**
     * 获取 Frosted Heart 团队 ID。
     * <p>
     * Gets the Frosted Heart team ID.
     *
     * @return 团队 ID / the team ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取团队所有者的玩家名称。
     * <p>
     * Gets the player name of the team owner.
     *
     * @return 所有者名称 / the owner's name
     */
    public String getOwnerName() {
        return ownerName;
    }
    /**
     * 获取关联的团队实例。
     * <p>
     * Gets the associated team instance.
     *
     * @return 团队实例 / the team instance
     */
    public AbstractTeam getTeam() {
        return team;
    }
    
    /**
     * 设置团队所有者的玩家名称。
     * <p>
     * Sets the player name of the team owner.
     *
     * @param ownerName 所有者名称 / the owner's name
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * 设置关联的团队实例。
     * <p>
     * Sets the associated team instance.
     *
     * @param team 团队实例 / the team instance
     */
    public void setTeam(AbstractTeam team) {
        this.team = team;
    }
	
	/**
	 * 获取所有在线成员。
	 * <p>
	 * Gets all online members.
	 *
	 * @return 在线成员集合 / the collection of online members
	 */
	public Collection<ServerPlayer> getOnlineMembers() {
		return team.getOnlineMembers();
	}
	Map<SpecialDataType,TeamDataClosure> dataHolderCache=new HashMap<>();
	/**
	 * 获取指定数据类型的 {@link TeamDataClosure}，用于延迟获取数据。使用缓存避免重复创建。
	 * <p>
	 * Gets a {@link TeamDataClosure} for the specified data type, for lazy data retrieval. Uses caching to avoid redundant creation.
	 *
	 * @param <U> 数据组件类型 / the data component type
	 * @param cap 数据组件的类型定义 / the data component type definition
	 * @return 团队数据闭包 / the team data closure
	 */
	public synchronized <U extends SpecialData> TeamDataClosure<U> getDataHolder(SpecialDataType<U> cap){
		return dataHolderCache.computeIfAbsent(cap, t->new TeamDataClosure<>(this,t));
	}

}
