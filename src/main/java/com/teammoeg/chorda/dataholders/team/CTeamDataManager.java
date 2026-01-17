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
 * The data manager for all team data.
 * use {@link CClientTeamDataManager} to get data in client
 * Normally, use
 * get(PlayerEntity player) to get the data for a player's team.
 * get(Team team) to get the data for FTB team.
 */
public class CTeamDataManager {

    public static CTeamDataManager INSTANCE;

    private static final LevelResource dataFolder = new LevelResource("chorda_data");
    private Map<UUID, UUID> dataByFTBId = new HashMap<>();
    private Map<UUID, TeamDataHolder> dataByOwnId = new HashMap<>();
    private Map<UUID, UUID> playerOwnedTeam;
    private Map<String,UUID> playerNameOwnedTeam;
    
    public CTeamDataManager() {
        INSTANCE = this;
    }

    /**
     * Get all data of all teams.
     * @return the data collection
     */
    public Collection<TeamDataHolder> getAllData() {
        return dataByOwnId.values();
    }

    /**
     * Get all data of all teams of a specific type.
     * @param type the type
     * @param <T> the type
     * @return the data stream
     */
    public <T extends SpecialData> Stream<T> getAllData(SpecialDataType<T> type) {
        return dataByOwnId.values().stream().map(t->t.getOptional(type)).filter(Optional::isPresent).map(Optional::get);
    }
    public <T extends SpecialData> void forAllData(SpecialDataType<T> type,BiConsumer<T,TeamDataHolder> consumer) {
        dataByOwnId.values().stream().forEach(t->{
        	Optional<T> opt=t.getOptional(type);
        	if(opt.isPresent()) {
        		consumer.accept( opt.get(),t);
        	}
        	
        });
    }



    /**
     * Helper method to get the data from frostedheart team id.
     * @apiNote DO NOT CALL IN CLIENT, or it would return empty data
     * @param id the research team id
     * @return data
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
     * Get the data for a player's team, should not call in client
     * @param player the player
     * @return the data
     */
	public static TeamDataHolder get(Player player) {
		AbstractTeam team=TeamsAPI.getAPI().getTeamByPlayer((ServerPlayer)player);
		if(team==null)return null;
		return INSTANCE.get(team);
		
	}
	public TeamDataHolder tryLoad(TeamDataHolder team) {
		if(team==null)return null;
		team.loadIfNeeded();
		return team;
	}
    /**
     * Get the data for a team, as well as check ownership and transfer if necessary.
     * @param team the team
     * @return the data
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
     * Get the data for a team from the frostedheart team id.
     *
     * @param id the frostedheart team id
     * @return the data
     */
    @Nullable
    public TeamDataHolder get(UUID id) {
        return tryLoad(dataByOwnId.get(id));
    }

    /**
     * Load all data from disk.
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
     * Save all data to disk.
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
     * Transfer data from one team to another.
     *
     * @param orig the original team id
     * @param team the new team
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
    public void transferByRid(UUID rid, AbstractTeam team) {
        TeamDataHolder odata = dataByOwnId.get(rid);
        if (odata != null) {
            odata.setTeam(team);
            odata.setOwnerName(CDistHelper.getServer().getProfileCache().get(team.getOwner()).map(GameProfile::getName).orElse(null));
            dataByFTBId.put(team.getId(), rid);
        }
    }


}
