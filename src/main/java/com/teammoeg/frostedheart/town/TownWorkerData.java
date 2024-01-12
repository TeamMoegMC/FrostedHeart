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

package com.teammoeg.frostedheart.town;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class TownWorkerData {
    private TownWorkerType type;
    private BlockPos pos;
    private CompoundNBT workData;
    private int priority;
    boolean loaded;

    public TownWorkerData(BlockPos pos) {
        super();
        this.pos = pos;
    }

    public TownWorkerData(CompoundNBT data) {
        super();
        this.pos = BlockPos.fromLong(data.getLong("pos"));
        this.type = TownWorkerType.valueOf(data.getString("type"));
        this.workData = data.getCompound("data");
        this.priority = data.getInt("priority");
    }

    public boolean afterWork(Town resource) {
        return type.getWorker().afterWork(resource, workData);
    }

    public boolean beforeWork(Town resource) {
        return type.getWorker().beforeWork(resource, workData);
    }

    public boolean firstWork(Town resource) {
        return type.getWorker().firstWork(resource, workData);
    }

    public void fromBlock(ITownBlockTE te) {
        type = te.getWorker();
        workData = te.getWorkData();
        priority = te.getPriority();
    }

    public BlockPos getPos() {
        return pos;
    }

    public long getPriority() {
        long prio = (priority & 0xFFFFFFFF) << 32 + (type.getPriority() & 0xFFFFFFFF);
        return prio;
    }

    public TownWorkerType getType() {
        return type;
    }

    public CompoundNBT getWorkData() {
        return workData;
    }

    public boolean lastWork(Town resource) {
        return type.getWorker().lastWork(resource, workData);
    }

    public CompoundNBT serialize() {
        CompoundNBT data = new CompoundNBT();
        data.putLong("pos", pos.toLong());
        data.putString("type", type.name());
        data.put("data", workData);
        data.putInt("priority", priority);
        return data;
    }

    public void setData(ServerWorld w) {
        if (loaded) {
            TileEntity te = Utils.getExistingTileEntity(w, pos);
            if (te instanceof ITownBlockTE) {
                ((ITownBlockTE) te).setWorkData(workData);
            }
        }
    }

    public void setWorkData(CompoundNBT workData) {
        this.workData = workData;
    }

    public boolean work(Town resource) {
        return type.getWorker().work(resource, workData);
    }
}
