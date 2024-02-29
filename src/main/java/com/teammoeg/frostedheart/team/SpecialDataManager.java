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

package com.teammoeg.frostedheart.team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.NBTSerializable;
import com.teammoeg.frostedheart.util.OptionalLazy;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.storage.FolderName;

public class SpecialDataManager {
    public static MinecraftServer server;
    static final FolderName dataFolder = new FolderName("fhdata");
    static final FolderName OlddataFolder = new FolderName("fhresearch");
    public static SpecialDataManager INSTANCE;
    private Map<UUID, UUID> dataByFTBId = new HashMap<>();
    private Map<UUID, TeamDataHolder> dataByResearchId = new HashMap<>();
    
    public static RecipeManager getRecipeManager() {
        if (server != null)
            return server.getRecipeManager();
        return ClientUtils.mc().world.getRecipeManager();
    }

    public SpecialDataManager(MinecraftServer s) {
        server = s;
        INSTANCE = this;
    }

    public Collection<TeamDataHolder> getAllData() {
        return dataByResearchId.values();
    }
    public <T extends NBTSerializable> Stream<T> getAllData(SpecialDataType<T,TeamDataHolder> type) {
        return dataByResearchId.values().stream().map(t->t.getOptional(type)).filter(t->t.isPresent()).map(t->t.get());
    }
    public static TeamDataHolder getDataByTeam(Team team) {
    	return INSTANCE.getData(team);
    }
    public static TeamDataHolder getDataByRid(UUID id) {
    	return INSTANCE.getData(id);
    }

	public static TeamDataHolder getData(PlayerEntity player) {
		return INSTANCE.getData(FTBTeamsAPI.getPlayerTeam((ServerPlayerEntity)player));
	}
    /*
     * get Team data as well as check ownership.
     *
     * */
    public TeamDataHolder getData(Team team) {
        UUID cn = dataByFTBId.get(team.getId());
        if (cn == null) {
            GameProfile owner = server.getPlayerProfileCache().getProfileByUUID(team.getOwner());
            
            if (owner != null)
                for (Entry<UUID, TeamDataHolder> dat : dataByResearchId.entrySet()) {
                    if (owner.getName().equals(dat.getValue().getOwnerName())) {
                        this.transfer(dat.getKey(), team);
                        break;
                    }
                }
        }
        cn = dataByFTBId.computeIfAbsent(team.getId(), t->UUID.randomUUID());
        TeamDataHolder data=dataByResearchId.computeIfAbsent(cn, c -> new TeamDataHolder(c, OptionalLazy.of(()->team)));
        if ((server.isSinglePlayer()||!server.isServerInOnlineMode())&&data.getOwnerName() == null) {
            PlayerProfileCache cache = server.getPlayerProfileCache();
            if (cache != null) {
                GameProfile gp = cache.getProfileByUUID(team.getOwner());
                if (gp != null) {
                	data.setOwnerName(gp.getName());
                }
            }
        }
        return data;

    }
    @Nullable
    public TeamDataHolder getData(UUID id) {

    	TeamDataHolder cn = dataByResearchId.get(id);
        return cn;

    }

    public void load() {
        dataByFTBId.clear();
        Path local=server.func_240776_a_(dataFolder);
        local.toFile().mkdirs();
        Path olocal=server.func_240776_a_(OlddataFolder);
        Stream.concat(Arrays.stream(olocal.toFile().listFiles((f) -> f.getName().endsWith(".nbt"))),Arrays.stream(local.toFile().listFiles((f) -> f.getName().endsWith(".nbt"))))
        .forEach(f->{
            UUID tud;
            try {
                try {
                    tud = UUID.fromString(f.getName().split("\\.")[0]);
                } catch (IllegalArgumentException ex) {
                    FHMain.LOGGER.error("Unexpected data file " + f.getName() + ", ignoring...");
                    return;
                }
                
                CompoundNBT nbt = CompressedStreamTools.readCompressed(f);
                if(nbt.contains("teamId"))
                	tud=nbt.getUniqueId("teamId");
                final UUID ftbid=tud;
                TeamDataHolder trd = new TeamDataHolder(nbt.getUniqueId("uuid"),OptionalLazy.of(() -> TeamManager.INSTANCE.getTeamByID(ftbid)));

                trd.deserialize(nbt, false);
                dataByFTBId.put(ftbid, trd.getId());
                dataByResearchId.put(trd.getId(), trd);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                FHMain.LOGGER.error("Unexpected data file " + f.getName() + ", ignoring...");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                FHMain.LOGGER.error("Unable to read data file " + f.getName() + ", ignoring...");
            }
        });
    }

    public void save() {
    	Path local=server.func_240776_a_(dataFolder);
        Set<String> files = new HashSet<>(Arrays.asList(local.toFile().list((d, s) -> s.endsWith(".nbt"))));
        for (Entry<UUID, TeamDataHolder> entry : dataByResearchId.entrySet()) {
            String fn = entry.getKey().toString() + ".nbt";
            File f = local.resolve(fn).toFile();
            try {
                CompressedStreamTools.writeCompressed(entry.getValue().serialize(false), f);
                files.remove(fn);
            } catch (IOException e) {

                e.printStackTrace();
                FHMain.LOGGER.error("Unable to save data file for team " + entry.getKey().toString() + ", ignoring...");
            }
        }
        for (String todel : files) {
            local.resolve(todel).toFile().delete();
        }
        Path olocal=server.func_240776_a_(OlddataFolder);
        if(olocal.toFile().exists()) {
        	try {
				FileUtils.deleteDirectory(olocal.toFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    public void transfer(UUID orig, Team team) {
    	UUID rid=dataByFTBId.remove(orig);
        TeamDataHolder odata = dataByResearchId.remove(rid);
        if (odata != null) {
            odata.setTeam(OptionalLazy.of(()->team));
            odata.setOwnerName(server.getPlayerProfileCache().getProfileByUUID(team.getOwner()).getName());
        }
        dataByFTBId.put(team.getId(), rid);

    }

}
