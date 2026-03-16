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

package com.teammoeg.chorda.dataholders.team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.chorda.events.TeamCreatedEvent;
import com.teammoeg.chorda.util.CDistHelper;
import com.mojang.authlib.GameProfile;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;

/**
 * 所有团队数据的服务端数据管理器。负责团队数据的加载、保存、传输和生命周期管理。
 * 客户端应使用 {@link CClientTeamDataManager} 获取数据。
 * 通常使用 {@link #get(Player)} 获取玩家所在团队的数据，或使用 {@link #get(AbstractTeam)} 获取指定团队的数据。
 * <p>
 * Server-side data manager for all team data. Handles loading, saving, transferring, and lifecycle management of team data.
 * Use {@link CClientTeamDataManager} for client-side data access.
 * Typically use {@link #get(Player)} to get data for a player's team, or {@link #get(AbstractTeam)} to get data for a specific team.
 */
public class CTeamDataManager {

    public static CTeamDataManager INSTANCE;

    private static final LevelResource dataFolder = new LevelResource("chorda_data");
    private Map<UUID, UUID> dataByFTBId = new HashMap<>();
    private Map<UUID, TeamDataHolder> dataByOwnId = new HashMap<>();
    private Map<UUID, UUID> playerOwnedTeam;
    private Map<String,UUID> playerNameOwnedTeam;
    
    /**
     * 构造一个新的团队数据管理器并将自身设置为全局实例。
     * <p>
     * Constructs a new team data manager and sets itself as the global instance.
     */
    public CTeamDataManager() {
        INSTANCE = this;
    }

    /**
     * 获取所有团队的数据。
     * <p>
     * Gets data for all teams.
     *
     * @return 所有团队数据的集合 / the collection of all team data
     */
    public Collection<TeamDataHolder> getAllData() {
        return dataByOwnId.values();
    }

    /**
     * 获取所有团队中指定类型的数据组件流。
     * <p>
     * Gets a stream of data components of the specified type from all teams.
     *
     * @param <T> 数据组件类型 / the data component type
     * @param type 数据组件的类型定义 / the data component type definition
     * @return 数据组件的流 / a stream of data components
     */
    public <T extends SpecialData> Stream<T> getAllData(SpecialDataType<T> type) {
        return dataByOwnId.values().stream().map(t->t.getOptional(type)).filter(Optional::isPresent).map(Optional::get);
    }
    /**
     * 遍历所有团队中指定类型的数据组件，对每个存在的数据执行消费操作。
     * <p>
     * Iterates over data components of the specified type across all teams, applying the consumer to each present data.
     *
     * @param <T> 数据组件类型 / the data component type
     * @param type 数据组件的类型定义 / the data component type definition
     * @param consumer 对数据组件和团队数据持有者的消费操作 / the consumer for the data component and team data holder
     */
    public <T extends SpecialData> void forAllData(SpecialDataType<T> type,BiConsumer<T,TeamDataHolder> consumer) {
        dataByOwnId.values().stream().forEach(t->{
        	Optional<T> opt=t.getOptional(type);
        	if(opt.isPresent()) {
        		consumer.accept( opt.get(),t);
        	}
        	
        });
    }



    /**
     * 根据 Frosted Heart 团队 ID 获取团队数据的辅助方法。
     * 在客户端调用时会返回空数据作为回退。
     * <p>
     * Helper method to get team data by Frosted Heart team ID.
     * Returns empty data as a fallback when called on the client.
     *
     * @apiNote 请勿在客户端调用，否则会返回空数据 / DO NOT CALL IN CLIENT, or it would return empty data
     * @param id 研究团队 ID / the research team ID
     * @return 团队数据，或在客户端为空数据 / the team data, or empty data on client
     */
    @Nullable
    public static TeamDataHolder getDataByResearchID(UUID id) {
    	//Note: this method should only be called in server, but create ponder make things wrost
    	//So this is a fallback mechanic returning an empty data when called in client
    	if(INSTANCE==null) {
    		return new TeamDataHolder(UUID.randomUUID(),new ClientTeam());
    	}
    	return INSTANCE.get(id);
    }

    /**
     * 获取玩家所在团队的数据，不应在客户端调用。
     * <p>
     * Gets the data for a player's team. Should not be called on the client.
     *
     * @param player 玩家 / the player
     * @return 团队数据 / the team data
     */
	public static TeamDataHolder get(Player player) {
		AbstractTeam team=TeamsAPI.getAPI().getTeamByPlayer((ServerPlayer)player);
		if(team==null)return null;
		return INSTANCE.get(team);
		
	}
	/**
	 * 尝试加载团队数据（如果尚未加载）。
	 * <p>
	 * Attempts to load team data if it has not been loaded yet.
	 *
	 * @param team 团队数据持有者 / the team data holder
	 * @return 已加载的团队数据持有者，如果输入为 null 则返回 null / the loaded team data holder, or null if input is null
	 */
	public TeamDataHolder tryLoad(TeamDataHolder team) {
		if(team==null)return null;
		team.loadIfNeeded();
		return team;
	}
    /**
     * 获取团队的数据，同时检查所有权并在必要时进行转移。
     * 如果团队尚未关联数据，则创建新的数据持有者并触发 {@link TeamCreatedEvent} 事件。
     * <p>
     * Gets data for a team, checking ownership and transferring if necessary.
     * If the team has no associated data, creates a new data holder and fires a {@link TeamCreatedEvent}.
     *
     * @param team 团队 / the team
     * @return 团队数据 / the team data
     */
    public TeamDataHolder get(AbstractTeam team) {
        UUID cn = dataByFTBId.get(team.getId());
        if (cn == null) {
            cn=UUID.randomUUID();
            dataByFTBId.put(team.getId(), cn);
            /*
            GameProfile owner = CDistHelper.getServer().getProfileCache().get(team.getOwner()).orElse(null);
            //System.out.println(owner);
            if (owner != null&&(!CDistHelper.getServer().usesAuthentication()||CDistHelper.getServer().isSingleplayer()))
                for (Entry<UUID, TeamDataHolder> dat : dataByOwnId.entrySet()) {
                    if (owner.getName().equals(dat.getValue().getOwnerName())) {
                        this.transferByRid(dat.getKey(), team);
                        cn=dat.getKey();
                        break;
                    }
                }*/
        }
        TeamDataHolder data= tryLoad(dataByOwnId.get(cn));
        if(data==null) {
        	data=new TeamDataHolder(cn, team);
        	
        	dataByOwnId.put(cn, data);
        	MinecraftForge.EVENT_BUS.post(new TeamCreatedEvent(data));
        }
        if (data.getOwnerName() == null) {
        	//System.out.println("filling owner name");
            GameProfileCache cache = CDistHelper.getServer().getProfileCache();
           
            if (cache != null) {
                GameProfile gp = cache.get(team.getOwner()).orElse(null);
                if (gp != null) {
                	//System.out.println("filled owner name");
                	data.setOwnerName(gp.getName());
                }
            }
        }
        return data;

    }

    /**
     * 根据 Frosted Heart 团队 ID 获取团队数据。
     * <p>
     * Gets team data by its Frosted Heart team ID.
     *
     * @param id Frosted Heart 团队 ID / the Frosted Heart team ID
     * @return 团队数据，如果未找到则返回 null / the team data, or null if not found
     */
    @Nullable
    public TeamDataHolder get(UUID id) {
        return tryLoad(dataByOwnId.get(id));
    }

    /**
     * 从磁盘加载所有团队数据。读取数据目录下的所有 .nbt 文件并反序列化为团队数据。
     * <p>
     * Loads all team data from disk. Reads all .nbt files in the data directory and deserializes them into team data.
     */
    public void load() {
        
        Path local=CDistHelper.getServer().getWorldPath(dataFolder);
        local.toFile().mkdirs();
        Stream<File> strm1=null,strm2=null;
        //Compatible migration from old data folder
        if(local.toFile().exists())
        	strm1=Arrays.stream(local.toFile().listFiles((f) -> f.getName().endsWith(".nbt")));
        if(strm1!=null) {
        	dataByFTBId.clear();
        	dataByOwnId.clear();
	        strm1.forEach(f->{
	            UUID tud;
	            try {
	                try {
	                    tud = UUID.fromString(f.getName().split("\\.")[0]);
	                } catch (IllegalArgumentException ex) {
	                    Chorda.LOGGER.error("Unexpected data file " + f.getName() + ", ignoring...");
	                    return;
	                }
	                
	                CompoundTag nbt = NbtIo.readCompressed(f);
	                if(nbt.contains("teamId"))
	                	tud=nbt.getUUID("teamId");
	                final UUID ftbid=tud;
	                TeamDataHolder trd = new TeamDataHolder(nbt.getUUID("uuid"),TeamsAPI.getAPI().getTeamByUuid(ftbid));
	                trd.deserialize(nbt, false);
	                dataByFTBId.put(ftbid, trd.getId());
	                dataByOwnId.put(trd.getId(), trd);
                    
	                Chorda.LOGGER.debug("Data file for team " + trd.getId().toString() + " loaded.");
	            } catch (IllegalArgumentException ex) {
	                ex.printStackTrace();
	                Chorda.LOGGER.error("Unexpected data file " + f.getName() + ", ignoring...");
                } catch (IOException e) {
	                e.printStackTrace();
	                Chorda.LOGGER.error("Unable to read data file " + f.getName() + ", ignoring...");
	            }
	        });
        }
    }

    /**
     * 将所有团队数据保存到磁盘。序列化后写入 .nbt 文件，并删除不再存在的团队数据文件。
     * <p>
     * Saves all team data to disk. Serializes and writes to .nbt files, and deletes data files for teams that no longer exist.
     */
    public void save() {
    	Path local=CDistHelper.getServer().getWorldPath(dataFolder);
        Set<String> files = new HashSet<>(Arrays.asList(local.toFile().list((d, s) -> s.endsWith(".nbt"))));
        for (Entry<UUID, TeamDataHolder> entry : dataByOwnId.entrySet()) {
            String fn = entry.getKey().toString() + ".nbt";
            File f = local.resolve(fn).toFile();
            try {
                NbtIo.writeCompressed(entry.getValue().serialize(false), f);
                files.remove(fn);
                Chorda.LOGGER.debug("Data file for team " + entry.getKey().toString() + " saved.");
            } catch (IOException e) {
                Chorda.LOGGER.error("Unable to save data file for team " + entry.getKey().toString() + ", ignoring...");
                e.printStackTrace();
            }
        }
        for (String todel : files) {
            local.resolve(todel).toFile().delete();
        }
    }

    /**
     * 将数据从一个团队转移到另一个团队。更新所有者信息和团队关联。
     * <p>
     * Transfers data from one team to another. Updates ownership information and team association.
     *
     * @param orig 原始团队 ID / the original team ID
     * @param team 新的目标团队 / the new target team
     */
    public void transfer(UUID orig, AbstractTeam team) {
    	UUID rid=dataByFTBId.remove(orig);
    	UUID orid=dataByFTBId.get(team.getId());
        TeamDataHolder odata = dataByOwnId.get(rid);
        System.out.println("rid:"+rid+",orid:"+orid+",odata"+odata);
        if (odata != null) {
            odata.setTeam(team);
            odata.setOwnerName(CDistHelper.getServer().getProfileCache().get(team.getOwner()).map(GameProfile::getName).orElse(null));
            
            TeamDataHolder otdh=dataByOwnId.remove(orid);
            dataByFTBId.put(team.getId(), rid);
           
        }else {
        	this.get(team);
        }


    }
    /**
     * 根据 Frosted Heart 内部团队 ID 将数据转移到新团队。
     * <p>
     * Transfers data to a new team by Frosted Heart internal team ID.
     *
     * @param rid Frosted Heart 内部团队 ID / the Frosted Heart internal team ID
     * @param team 新的目标团队 / the new target team
     */
    public void transferByRid(UUID rid, AbstractTeam team) {
        TeamDataHolder odata = dataByOwnId.get(rid);
        if (odata != null) {
            odata.setTeam(team);
            odata.setOwnerName(CDistHelper.getServer().getProfileCache().get(team.getOwner()).map(GameProfile::getName).orElse(null));
            dataByFTBId.put(team.getId(), rid);
        }
    }


}
