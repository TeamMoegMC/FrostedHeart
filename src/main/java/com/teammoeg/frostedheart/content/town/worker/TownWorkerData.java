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

package com.teammoeg.frostedheart.content.town.worker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.WorkerResidentHandler;

import blusunrize.immersiveengineering.common.util.Utils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Data for a worker (town block) in the town.
 * <p>
 * A TownWorkerData is the basic data component in a TeamTownData.
 * It specifies the type of worker, the position of worker, the work data.
 * <p>
 * The work data is especially important, as it stores additional data that
 * should be synced with the entire town. It is an interface between the town
 * and the worker.
 * Work data consist of 2 parts: tileEntity and town. tileEntity stores the
 * data from the tile entity, and town stores the data from the town.
 * <p>
 * There can be multiple worker data with the same worker type.
 */
public class TownWorkerData {
	public static final Codec<TownWorkerData> CODEC=RecordCodecBuilder.create(t->
	t.group(CodecUtil.enumCodec(TownWorkerType.class).fieldOf("type").forGetter(o->o.type),
		BlockPos.CODEC.fieldOf("pos").forGetter(o->o.pos),
		CompoundTag.CODEC.fieldOf("data").forGetter(o->{CompoundTag tag=new CompoundTag();if(o.state!=null)o.state.writeNBT(tag,false);return tag;}),
		Codec.INT.optionalFieldOf("priority",0).forGetter(o->o.priority)
		).apply(t,TownWorkerData::new));
    public static final String KEY_IS_OVERLAPPED = "isOverlapped";
    @Getter
    private TownWorkerType type;
    @Getter
    private WorkerState state;
    @Getter
    private BlockPos pos;
    private int priority;
    public boolean loaded;
  
    public TownWorkerData(TownWorkerType type, BlockPos pos, int priority) {
        super();
        this.pos = pos;
        this.type=type;
        this.state=type.getWorker().createState();
        this.priority=priority;
    }

    public TownWorkerData(TownWorkerType type, BlockPos pos, CompoundTag workData, int priority) {
		super();
		this.type = type;
		this.pos = pos;
		this.state=type.getWorker().createState();
		this.state.readNBT(workData, false);
		this.priority = priority;
	}

	public TownWorkerData(CompoundTag data) {
        super();
        this.pos = BlockPos.of(data.getLong("pos"));
        this.type = TownWorkerType.valueOf(data.getString("type"));
		this.state=type.getWorker().createState();
		this.state.readNBT(data.getCompound("data"), false);
        this.priority = data.getInt("priority");
    }

    public long getPriority() {
        return (long) (priority) << 32 + (type.getPriority());
    }


    public CompoundTag serialize() {
        CompoundTag data = new CompoundTag();
        data.putLong("pos", pos.asLong());
        data.putString("type", type.name());
        CompoundTag workdata = new CompoundTag();
        state.writeNBT(workdata, false);
        data.put("data", workdata);
        data.putInt("priority", priority);
        return data;
    }

    public boolean work(Town resource,WorkOrder order) {
        return type.getWorker().work(resource, state, order);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TownWorkerData that)) return false;
        return priority == that.priority &&
                Objects.equals(type, that.type) &&
                Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, pos, priority);
    }

    /**
     * Get the residents of this worker.
     * Should ONLY be used if the worker holds residents, like house, mine, etc.
     * @return the residents, 且为UUID形式
     */
    public List<UUID> getResidents(){
        return state.getResidents();
    }

    /**
     * Get the max resident of this worker.
     * Should ONLY be used if the worker holds residents, like house, mine, etc.
     */
    public int getMaxResident(){
        if(!WorkerResidentHandler.isResidentWorker(this)){
            throw new IllegalArgumentException("Worker Type: " + this.type.name() + " does not hold residents!");
        }
        return state.maxResidents;
    }

    /**
     * Add a resident to this worker.
     * Should ONLY be used if the worker holds residents, like house, mine, etc.
     */
    public boolean addResident(UUID uuid){
        if(!WorkerResidentHandler.isResidentWorker(this)){
            throw new IllegalArgumentException("Worker Type: " + this.type.name() + " does not hold residents!");
        }
        return state.addResident(uuid);
    }

    /**
     * Remove a resident from this worker.
     * Should ONLY be used if the worker holds residents, like house, mine, etc.
     */
    public boolean removeResident(UUID uuid){
        if(!WorkerResidentHandler.isResidentWorker(this)){
            throw new IllegalArgumentException("Worker Type: " + this.type.name() + " does not hold residents!");
        }
        return state.removeResident(uuid);
    }

    //下面俩方法在别的地方检查了type的合法性，不需要在这里检查
    /**
     * Get priority when assigning work
     * Should ONLY be used if the worker holds residents, like house, mine, etc.
     */
    public double getResidentPriority(){
        return type.getResidentPriority(this);
    }

    public double getResidentPriority(int residentNum){
        return type.getResidentPriority(residentNum, this);
    }
    public void onRemove(ServerLevel level) {
    	type.getWorker().onRemoved(level, state, pos);
    }
}
