package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.blockentity.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.scheduler.ScheduledTaskTileEntity;
import com.teammoeg.frostedheart.base.scheduler.SchedulerQueue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class AbstractTownWorkerTileEntity extends FHBaseTileEntity implements
        TownTileEntity, ScheduledTaskTileEntity, FHBlockInterfaces.IActiveState {
    public TownWorkerState workerState = TownWorkerState.NOT_INITIALIZED;
    public OccupiedArea occupiedArea;
    protected boolean addedToSchedulerQueue = false;
    public AbstractTownWorkerTileEntity(BlockEntityType<? extends BlockEntity> type)  {
        super(type);
    }
    public abstract void refresh();
    protected boolean isAddedToSchedulerQueue = false;

    public void executeTask(){
        this.refresh();
    }

    public boolean isStillValid(){
        return true;
    }

    @Override
    public OccupiedArea getOccupiedArea() {
        if(this.isWorkValid()) return occupiedArea;
        return OccupiedArea.EMPTY;
    }

    public static OccupiedArea getOccupiedArea(CompoundTag nbt){
        return OccupiedArea.fromNBT(nbt.getCompound("occupiedArea"));
    }

    public static OccupiedArea getOccupiedArea(TownWorkerData data){
        return getOccupiedArea(data.getWorkData());
    }

    @Override
    public boolean isWorkValid(){
        if(workerState== TownWorkerState.NOT_INITIALIZED) this.refresh();
        return workerState.isValid();
    }

    /**
     * doesn't refresh the tile entity if didn't initialize
     */
    public boolean isValid(){
        return this.workerState.isValid();
    }

    public static boolean isValid(CompoundTag nbt){
        return TownWorkerState.fromByte(nbt.getByte("workerState")).isValid();
    }

    public static boolean isValid(TownWorkerData data){
        return isValid(data.getWorkData());
    }

    public boolean isOccupiedAreaOverlapped(){
        return this.workerState == TownWorkerState.OCCUPIED_AREA_OVERLAPPED;}

    protected CompoundTag getBasicWorkData(){
        CompoundTag nbt = new CompoundTag();
        nbt.putByte("workerState", workerState.getStateNum());
        if(this.occupiedArea != null) nbt.put("occupiedArea", occupiedArea.toNBT());
        return nbt;
    }
    protected void setBasicWorkData(CompoundTag data){
        workerState = TownWorkerState.fromByte(data.getByte("workerState"));
        occupiedArea = OccupiedArea.fromNBT(data.getCompound("occupiedArea"));
    }

    public void setWorkerState(TownWorkerState workerState) {
        this.workerState = workerState;
    }
    public TownWorkerState getWorkerState() {
        return workerState;
    }

    public static TownWorkerState getWorkerState(CompoundTag nbt){
        return TownWorkerState.fromByte(nbt.getByte("workerState"));
    }

    public static TownWorkerState getWorkerState(TownWorkerData data){
        return getWorkerState(data.getWorkData());
    }

    public void addToSchedulerQueue(){
        if(!this.addedToSchedulerQueue){
            this.addedToSchedulerQueue = true;
            SchedulerQueue.add(this);
        }
    }

    public void tick(){
        this.addToSchedulerQueue();
    }

    //这两个方法除了在house里面，暂时没什么用
    @Override
    public void readCustomNBT(CompoundTag compoundNBT, boolean b) {}
    @Override
    public void writeCustomNBT(CompoundTag compoundNBT, boolean b) {}
}
