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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.Objects;

/**
 * Data for a worker (town block) in the town.
 * <p>
 * A TownWorkerData is the basic data component in a TeamTownData.
 * It specifies the type of worker, the position of worker, the work data.
 * <p>
 * The work data is especially important, as it stores additional data that
 * should be synced with the entire town. It is an interface between the town
 * and the worker.
 * <p>
 * There can be multiple worker data with the same worker type.
 */
public class TownWorkerData {
	public static final Codec<TownWorkerData> CODEC=RecordCodecBuilder.create(t->
	t.group(CodecUtil.enumCodec(TownWorkerType.class).fieldOf("type").forGetter(o->o.type),
		CodecUtil.BLOCKPOS.fieldOf("pos").forGetter(o->o.pos),
		CompoundNBT.CODEC.fieldOf("data").forGetter(o->o.workData),
		Codec.INT.fieldOf("priority").forGetter(o->o.priority)
		).apply(t,TownWorkerData::new));
    private TownWorkerType type;
    private BlockPos pos;
    private CompoundNBT workData;
    private int priority;
    boolean loaded;

    public TownWorkerData(BlockPos pos) {
        super();
        this.pos = pos;
    }

    public TownWorkerData(TownWorkerType type, BlockPos pos, CompoundNBT workData, int priority) {
		super();
		this.type = type;
		this.pos = pos;
		this.workData = workData;
		this.priority = priority;
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

    public void fromTileEntity(TownTileEntity te) {
        type = te.getWorkerType();
        workData = te.getWorkData();
        priority = te.getPriority();
    }

    public BlockPos getPos() {
        return pos;
    }

    public long getPriority() {
        return (long) (priority) << 32 + (type.getPriority());
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
            if (te instanceof TownTileEntity) {
                ((TownTileEntity) te).setWorkData(workData);
            }
        }
    }

    public void setWorkData(CompoundNBT workData) {
        this.workData = workData;
    }

    public void setWorkData(ServerWorld w){
        TileEntity te = Utils.getExistingTileEntity(w, pos);
        if (te instanceof TownTileEntity) {
            this.workData = ((AbstractTownWorkerTileEntity) te).getWorkData();
        }
    }

    public boolean work(Town resource) {
        return type.getWorker().work(resource, workData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TownWorkerData)) return false;
        TownWorkerData that = (TownWorkerData) o;
        return priority == that.priority &&
                Objects.equals(type, that.type) &&
                Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, pos, priority);
    }


}
