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

    public CompoundNBT serialize() {
        CompoundNBT data = new CompoundNBT();
        data.putLong("pos", pos.toLong());
        data.putString("type", type.name());
        data.put("data", workData);
        data.putInt("priority", priority);
        return data;
    }

    public void fromBlock(ITownBlockTE te) {
        type = te.getWorker();
        workData = te.getWorkData();
        priority = te.getPriority();
    }

    public TownWorkerType getType() {
        return type;
    }

    public BlockPos getPos() {
        return pos;
    }

    public CompoundNBT getWorkData() {
        return workData;
    }

    public void setWorkData(CompoundNBT workData) {
        this.workData = workData;
    }

    public boolean beforeWork(Town resource) {
        return type.getWorker().beforeWork(resource, workData);
    }

    public boolean work(Town resource) {
        return type.getWorker().work(resource, workData);
    }

    public boolean afterWork(Town resource) {
        return type.getWorker().afterWork(resource, workData);
    }

    public long getPriority() {
        long prio = (priority & 0xFFFFFFFF) << 32 + (type.getPriority() & 0xFFFFFFFF);
        return prio;
    }

    public void setData(ServerWorld w) {
        if (loaded) {
            TileEntity te = Utils.getExistingTileEntity(w, pos);
            if (te instanceof ITownBlockTE) {
                ((ITownBlockTE) te).setWorkData(workData);
            }
        }
    }

    public boolean firstWork(Town resource) {
        return type.getWorker().firstWork(resource, workData);
    }

    public boolean lastWork(Town resource) {
        return type.getWorker().lastWork(resource, workData);
    }
}
