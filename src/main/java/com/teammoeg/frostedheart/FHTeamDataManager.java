/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart;

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
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataType;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.utility.OptionalLazy;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The data manager for all team data.
 * use {@link FHClientTeamDataManager} to get data in client
 * Normally, use
 * get(PlayerEntity player) to get the data for a player's team.
 * get(Team team) to get the data for FTB team.
 */
public class FHTeamDataManager {

    public static FHTeamDataManager INSTANCE;
    private final MinecraftServer server;
    static final LevelResource dataFolder = new LevelResource("fhdata");
    static final LevelResource oldDataFolder = new LevelResource("fhresearch");
    private Map<UUID, UUID> dataByFTBId = new HashMap<>();
    private Map<UUID, TeamDataHolder> dataByFhId = new HashMap<>();
    
    public static RecipeManager getRecipeManager() {
        if (getServer() != null)
            return getServer().getRecipeManager();
        return ClientUtils.mc().level.getRecipeManager();
    }

    public FHTeamDataManager(MinecraftServer s) {
        server = s;
        INSTANCE = this;
    }

    /**
     * Get all data of all teams.
     * @return the data collection
     */
    public Collection<TeamDataHolder> getAllData() {
        return dataByFhId.values();
    }

    /**
     * Get all data of all teams of a specific type.
     * @param type the type
     * @param <T> the type
     * @return the data stream
     */
    public <T extends SpecialData> Stream<T> getAllData(SpecialDataType<T> type) {
        return dataByFhId.values().stream().map(t->t.getOptional(type)).filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Helper method to get the data from a team.
     * @param team the team
     * @return data
     */
    public static TeamDataHolder getDataByTeam(Team team) {
    	return INSTANCE.get(team);
    }

    /**
     * Helper method to get the data from frostedheart team id.
     * @param id the research team id
     * @return data
     */
    public static TeamDataHolder getDataByResearchID(UUID id) {
    	return INSTANCE.get(id);
    }

    /**
     * Get the data for a player's team, should not call in client
     * @param player the player
     * @return the data
     */
	public static TeamDataHolder get(Player player) {
		return INSTANCE.get(FTBTeamsAPI.getPlayerTeam((ServerPlayer)player));
		
	}

    /**
     * Get the data for a team, as well as check ownership and transfer if necessary.
     * @param team the team
     * @return the data
     */
    public TeamDataHolder get(Team team) {
        UUID cn = dataByFTBId.get(team.getId());
        if (cn == null) {
            cn=UUID.randomUUID();
            dataByFTBId.put(team.getId(), cn);
            GameProfile owner = getServer().getProfileCache().get(team.getOwner());
            
            if (owner != null&&(!getServer().usesAuthentication()||getServer().isSingleplayer()))
                for (Entry<UUID, TeamDataHolder> dat : dataByFhId.entrySet()) {
                    if (owner.getName().equals(dat.getValue().getOwnerName())) {
                        this.transferByRid(dat.getKey(), team);
                        cn=dat.getKey();
                        break;
                    }
                }
        }
        TeamDataHolder data=dataByFhId.computeIfAbsent(cn, c -> new TeamDataHolder(c, OptionalLazy.of(()->team)));
        if (data.getOwnerName() == null) {
            GameProfileCache cache = getServer().getProfileCache();
            if (cache != null) {
                GameProfile gp = cache.get(team.getOwner());
                if (gp != null) {
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
        return dataByFhId.get(id);
    }

    /**
     * Load all data from disk.
     */
    public void load() {
        
        Path local=getServer().getWorldPath(dataFolder);
        local.toFile().mkdirs();
        Path olocal=getServer().getWorldPath(oldDataFolder);
        Stream<File> strm1=null,strm2=null;
        //Compatible migration from old data folder
        if(local.toFile().exists())
        	strm1=Arrays.stream(local.toFile().listFiles((f) -> f.getName().endsWith(".nbt")));
        if(olocal.toFile().exists())
        	strm2=Arrays.stream(olocal.toFile().listFiles((f) -> f.getName().endsWith(".nbt")));
        if(strm1!=null) {
        	if(strm2!=null)
        		strm1=Stream.concat(strm1, strm2);
        }else {
        	if(strm2!=null)
        		strm1=strm2;
        }
        if(strm1!=null) {
        	dataByFTBId.clear();
        	dataByFhId.clear();
	        strm1.forEach(f->{
	            UUID tud;
	            try {
	                try {
	                    tud = UUID.fromString(f.getName().split("\\.")[0]);
	                } catch (IllegalArgumentException ex) {
	                    FHMain.LOGGER.error("Unexpected data file " + f.getName() + ", ignoring...");
	                    return;
	                }
	                
	                CompoundTag nbt = NbtIo.readCompressed(f);
	                if(nbt.contains("teamId"))
	                	tud=nbt.getUUID("teamId");
	                final UUID ftbid=tud;
	                TeamDataHolder trd = new TeamDataHolder(nbt.getUUID("uuid"),OptionalLazy.of(() -> TeamManager.INSTANCE.getTeamByID(ftbid)));
	
	                trd.deserialize(nbt, false);
	                dataByFTBId.put(ftbid, trd.getId());
	                dataByFhId.put(trd.getId(), trd);
	            } catch (IllegalArgumentException ex) {
	                ex.printStackTrace();
	                FHMain.LOGGER.error("Unexpected data file " + f.getName() + ", ignoring...");
                } catch (IOException e) {
	                e.printStackTrace();
	                FHMain.LOGGER.error("Unable to read data file " + f.getName() + ", ignoring...");
	            }
	        });
        }
    }

    /**
     * Save all data to disk.
     */
    public void save() {
    	Path local=getServer().getWorldPath(dataFolder);
        Set<String> files = new HashSet<>(Arrays.asList(local.toFile().list((d, s) -> s.endsWith(".nbt"))));
        for (Entry<UUID, TeamDataHolder> entry : dataByFhId.entrySet()) {
            String fn = entry.getKey().toString() + ".nbt";
            File f = local.resolve(fn).toFile();
            try {
                NbtIo.writeCompressed(entry.getValue().serialize(false), f);
                files.remove(fn);
            } catch (IOException e) {

                e.printStackTrace();
                FHMain.LOGGER.error("Unable to save data file for team " + entry.getKey().toString() + ", ignoring...");
            }
        }
        for (String todel : files) {
            local.resolve(todel).toFile().delete();
        }
        Path olocal=getServer().getWorldPath(oldDataFolder);
        if(olocal.toFile().exists()) {
        	try {
				FileUtils.deleteDirectory(olocal.toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    /**
     * Transfer data from one team to another.
     *
     * @param orig the original team id
     * @param team the new team
     */
    public void transfer(UUID orig, Team team) {
    	UUID rid=dataByFTBId.remove(orig);
        TeamDataHolder odata = dataByFhId.get(rid);
        if (odata != null) {
            odata.setTeam(OptionalLazy.of(()->team));
            odata.setOwnerName(getServer().getProfileCache().get(team.getOwner()).getName());
            dataByFTBId.put(team.getId(), rid);
        }else {
        	this.get(team);
        }


    }
    public void transferByRid(UUID rid, Team team) {
        TeamDataHolder odata = dataByFhId.get(rid);
        if (odata != null) {
            odata.setTeam(OptionalLazy.of(()->team));
            odata.setOwnerName(getServer().getProfileCache().get(team.getOwner()).getName());
            dataByFTBId.put(team.getId(), rid);
        }
    }
    /**
     * Get the server instance.
     * @return the server instance
     */
	public static MinecraftServer getServer() {
		if(INSTANCE==null)
			return null;
		return INSTANCE.server;
	}

}
