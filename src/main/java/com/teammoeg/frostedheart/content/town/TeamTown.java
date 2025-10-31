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

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.action.ITownResourceActionExecutorHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

/**
 * The town for a player team.
 * <p>
 * The TeamTown is only an interface of the underlying TeamTownData.
 * You may use this to access or modify town data.
 */
public class TeamTown implements Town, ITownWithResidents, ITownWithBlocks {

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
     * Default constructor links storage to the town data.
     * @param td the town data
     */
    public TeamTown(TeamTownData td) {
        super();
        this.data = td;
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
    public ITownResourceActionExecutorHandler getActionExecutorHandler() {
        return data.resources.actionExecutor;
    }

    public TeamTownResourceHolder getResourceHolder() {
        return data.resources;
    }

    @Override
    public Optional<TeamTownData> getTownData() {
        return Optional.of(data);
    }
}
