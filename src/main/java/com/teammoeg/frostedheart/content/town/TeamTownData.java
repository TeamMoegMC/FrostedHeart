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

import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.team.TeamDataHolder;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

/**
 * Town data for a whole team.
 * <p>
 * It maintains town resources, worker data, and holds a team data
 * when initialized.
 * <p>
 * Everything permanent should be saved in this class.
 */
public class TeamTownData implements NBTSerializable{
    /**
     * The town name.
     */
    String name;
    /**
     * The town residents.
     */
    Map<UUID, Resident> residents = new LinkedHashMap<>();
	/**
     * Resource generated from resident
     */
    Map<TownResourceType, Integer> resources = new EnumMap<>(TownResourceType.class);
    /**
     * Resource provided by player
     */
    Map<TownResourceType, Integer> backupResources = new EnumMap<>(TownResourceType.class);
    /**
     * Town blocks and their worker data
     */
    Map<BlockPos, TownWorkerData> blocks = new LinkedHashMap<>();
    /**
     * The team data pointer
     */
    TeamDataHolder teamData;

    public TeamTownData(TeamDataHolder teamData) {
        super();
        this.teamData = teamData;
        if (teamData.getTeam().isPresent()) {
            this.name = teamData.getTeam().get().getDisplayName() + "'s Town";
        } else {
            this.name = teamData.getOwnerName() + "'s Town";
        }
    }

    /**
     * Town logic update (every 20 ticks).
     * This method first validates the town blocks, then sorts them by priority and calls the work methods.
     *
     * @param world server world instance
     */
    public void tick(ServerWorld world) {
        PriorityQueue<TownWorkerData> pq = new PriorityQueue<>(Comparator.comparingLong(TownWorkerData::getPriority).reversed());
        blocks.values().removeIf(v -> {
            BlockPos pos = v.getPos();
            v.loaded = false;
            if (world.isBlockLoaded(pos)) {
                v.loaded = true;
                BlockState bs = world.getBlockState(pos);
                TileEntity te = Utils.getExistingTileEntity(world, pos);
                TownWorkerType twt = v.getType();
                return twt.getBlock() != bs.getBlock() || te == null || !(te instanceof TownTileEntity) || !((TownTileEntity) te).isWorkValid();
            }
            return false;
        });
        for (TownWorkerData v : blocks.values()) {
            pq.add(v);
        }
        TeamTown itt = new TeamTown(this);
        for (TownWorkerData t : pq) {
            t.firstWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.beforeWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.work(itt);
        }
        for (TownWorkerData t : pq) {
            t.afterWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.lastWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.setData(world);
        }
        itt.finishWork();
    }


	@Override
	public void save(CompoundNBT nbt, boolean isPacket) {
        ListNBT list = new ListNBT();
        for (TownWorkerData v : blocks.values()) {
            list.add(v.serialize());
        }
        nbt.put("blocks", list);
        
        CompoundNBT list2 = new CompoundNBT();
        for (Entry<TownResourceType, Integer> v : resources.entrySet()) {
            if (v.getValue() != null && v.getValue() != 0)
                list2.putInt(v.getKey().getKey(), v.getValue());

        }
        nbt.put("resource", list2);
        CompoundNBT list3 = new CompoundNBT();
        for (Entry<TownResourceType, Integer> v : backupResources.entrySet()) {
            if (v.getValue() != null && v.getValue() != 0)
                list3.putInt(v.getKey().getKey(), v.getValue());
        }
        nbt.put("backupResource", list3);
        nbt.putString("name", name);
	}


	@Override
	public void load(CompoundNBT data, boolean isPacket) {
        for (INBT i : data.getList("blocks", Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT nbt = (CompoundNBT) i;
            TownWorkerData t = new TownWorkerData(nbt);
            blocks.put(t.getPos(), t);
        }
        CompoundNBT rec = data.getCompound("resource");
        for (String i : rec.keySet()) {
            resources.put(TownResourceType.from(i), rec.getInt(i));
        }
        CompoundNBT rec2 = data.getCompound("backupResource");
        for (String i : rec2.keySet()) {
            backupResources.put(TownResourceType.from(i), rec2.getInt(i));
        }
        name = data.getString("name");
	}


}
