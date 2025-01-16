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

package com.teammoeg.frostedheart.content.town;

import java.util.*;
import java.util.Map.Entry;

import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import com.teammoeg.frostedheart.content.town.resource.TownResourceManager;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

/**
 * The town for a player team.
 * <p>
 * The TeamTown is only an interface of the underlying TeamTownData.
 * You may use this to access or modify town data.
 */
public class TeamTown implements Town, TownWithResident {
    /** Used to manage town resource. */
    TownResourceManager resources;
    /** The town data, actual data stored on disk. */
    TeamTownData data;

    /**
     * Create a new town based on data.
     * @param data can be taken from a player, team, etc.
     *             can also be experimental data.
     * @return the town
     */
    public static TeamTown create(TeamTownData data) {
        return new TeamTown(data);
    }

    /**
     * Get the town for a player.
     * @param player the player
     * @return the town
     */
    public static TeamTown from(Player player) {
        TeamTownData data = CTeamDataManager.get(player).getData(FHSpecialDataTypes.TOWN_DATA);
        return new TeamTown(data);
    }

    /**
     * Get the town for a team.
     * @param team the team
     * @return the town
     */
    public static TeamTown from(Team team) {
        TeamTownData data = CTeamDataManager.getDataByTeam(team).getData(FHSpecialDataTypes.TOWN_DATA);
        return new TeamTown(data);
    }

    /**
     * Default constructor links storage to the town data.
     * @param td the town data
     */
    public TeamTown(TeamTownData td) {
        super();
        this.resources = new TownResourceManager(td.resources);
        this.data = td;
    }

    /*
    @Override
    public double add(TownResourceType name, double val, boolean simulate) {
        int newVal = resources.getOrDefault(name, 0);
        newVal += (int) (val * 1000);
        int max = getIntMaxStorage(name) - backupStorage.getOrDefault(name, 0);
        int remain = 0;
        if (newVal > max) {
            remain = newVal - max;
            newVal = max;
        }
        if (!simulate)
            resources.put(name, newVal);
        return val - remain / 1000d;
    }

    @Override
    public double addService(TownResourceType name, double val) {
        resources.merge(name, (int) val * 1000, Integer::sum);
        return val;
    }

    @Override
    public double cost(TownResourceType name, double val, boolean simulate) {
        int servVal = service.getOrDefault(name, 0);
        int curVal = resources.getOrDefault(name, 0);
        int buVal = backupStorage.getOrDefault(name, 0);
        int remain = 0;
        servVal -= (int) (val * 1000);
        if (servVal < 0) {
            curVal += servVal;
            servVal = 0;
        }
        if (curVal < 0) {
            buVal += curVal;
            curVal = 0;
        }
        if (buVal < 0) {
            remain = -buVal;
            buVal = 0;
        }
        if (!simulate) {
            if (servVal > 0) {
                service.put(name, servVal);
            } else {
                service.remove(name);
            }
            if (curVal > 0) {
                resources.put(name, curVal);
            } else {
                resources.remove(name);
            }
            if (buVal > 0) {
                backupStorage.put(name, buVal);
            } else {
                backupStorage.remove(name);
            }
        }
        return val - remain / 1000d;
    }

    @Override
    public double costAsService(TownResourceType name, double val, boolean simulate) {
        int toCost = (int) (val * 1000);
        int curVal = resources.getOrDefault(name, 0);
        //int buVal=backupStorage.getOrDefault(name, 0);
        int servVal = service.getOrDefault(name, 0);
        int remain = 0;
        int costedRC = 0;
        servVal -= toCost;
        if (servVal < 0) {
            costedRC = -servVal;
            curVal += servVal;
            servVal = 0;
        }
        if (curVal < 0) {
            remain = -curVal;
            curVal = 0;
        }
		
		//if(buVal<0) {
		//	remain=-buVal;
		//	buVal=0;
		//}
        costedRC -= remain;
        int costed = toCost - remain;
        if (!simulate) {
            if (servVal > 0) {
                service.put(name, servVal);
            } else {
                service.remove(name);
            }
            if (curVal > 0) {
                resources.put(name, curVal);
            } else {
                resources.remove(name);
            }
			//if(buVal>0) {
			//	backupStorage.put(name, buVal);
			//}else {
			//	backupStorage.remove(name);
			//}
            if (costedRC > 0) {
                costedRC += costedService.getOrDefault(name, 0);
                costedService.put(name, costedRC);
            }
        }
        return costed / 1000d;
    }

    @Override
    public double costService(TownResourceType name, double val, boolean simulate) {
        int servVal = service.getOrDefault(name, 0);
        int remain = 0;
        int ival = (int) (val * 1000);
        if (servVal >= ival) {
            servVal -= ival;
        } else {
            remain = ival - servVal;
            servVal = 0;
        }
        if (!simulate) {
            if (servVal > 0) {
                service.put(name, servVal);
            } else {
                service.remove(name);
            }
        }
        return val - remain / 1000d;
    }

    public void finishWork() {
        resources.putAll(costedService);
        for (Entry<TownResourceType, Integer> ent : resources.entrySet()) {
            int max = getIntMaxStorage(ent.getKey());
            if (ent.getValue() > max) {
                ent.setValue(max);
            }
        }
    }


    public double get(TownResourceType name) {
        int val = resources.getOrDefault(name, 0);
        val += backupStorage.getOrDefault(name, 0);
        val += service.getOrDefault(name, 0);

        return val / 1000d;
    }
    */

    /**
     * Get the resource manager.
     * Use methods in TownResourceManager to change resources in town.
     * @return the town resource manager
     */
    public TownResourceManager getResourceManager() {
        return resources;
    }


    /**
     * Get the blocks and their worker data.
     */
    public Map<BlockPos, TownWorkerData> getTownBlocks() {
        return data.blocks;
    }

    /**
     * Get the work data of the town block. (Not the TownWorkerData)
     * @param pos position of the block
     * @return the work data
     */
    public CompoundTag getTownBlockData(BlockPos pos) {
        TownWorkerData twd = data.blocks.get(pos);
        if (twd == null)
            return null;
        return twd.getWorkData();
    }

    /**
     * Initializes new TownWorkerData from the tile entity.
     * Put the data into the map.
     *
     * @param pos position of the block
     * @param tile the tile entity associated with the block
     */
    public void addTownBlock(BlockPos pos, TownBlockEntity tile) {
        TownWorkerData workerData = data.blocks.computeIfAbsent(pos, TownWorkerData::new);
        workerData.fromTileEntity(tile);
    }

    /**
     * Remove the town block from the map.
     *
     * @param pos position of the block
     */
    public void removeTownBlock(BlockPos pos) {
        data.blocks.remove(pos);
    }

    public Map<UUID, Resident> getResidents() {
        return data.residents;
    }

    public Collection<Resident> getAllResidents(){
        return data.residents.values();
    }

    public Optional<Resident> getResident(UUID id){
        return Optional.of(data.residents.get(id));
    }

    public void addResident(Resident resident) {
        data.residents.put(resident.getUUID(), resident);
    }

    public void addResident(String firstName, String lastName) {
        addResident(new Resident(firstName, lastName));
    }

    public void removeResident(UUID id) {
        data.residents.remove(id);
    }

    /**
     * Remove all resident matching the first and last name.
     * @param firstName the first name
     * @param lastName the last name
     * @return true if the resident was removed
     */
    public boolean removeResident(String firstName, String lastName) {
        boolean removed = false;
        for (Entry<UUID, Resident> entry : getResidents().entrySet()) {
            if (entry.getValue().getFirstName().equals(firstName) && entry.getValue().getLastName().equals(lastName)) {
                getResidents().remove(entry.getKey());
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Get the town name.
     */
    public String getName() {
        return data.name;
    }

    /**
     * Set the town name.
     * @param name the new name
     */
    public void setName(String name) {
        this.data.name = name;
    }

    @Override
    public Optional<TeamTownData> getTownData() {
        return Optional.of(data);
    }
}
