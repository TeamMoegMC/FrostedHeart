package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.scheduler.ScheduledTaskTileEntity;
import com.teammoeg.frostedheart.base.scheduler.SchedulerQueue;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import static com.teammoeg.frostedheart.content.town.TownWorkerState.NOT_INITIALIZED;

public abstract class AbstractTownWorkerTileEntity extends FHBaseTileEntity implements
        TownTileEntity, ScheduledTaskTileEntity, ITickableTileEntity, FHBlockInterfaces.IActiveState {
    public TownWorkerState workerState = NOT_INITIALIZED;
    public OccupiedArea occupiedArea;
    protected boolean addedToSchedulerQueue = false;
    public AbstractTownWorkerTileEntity(TileEntityType<? extends TileEntity> type)  {
        super(type);
    }
    public abstract void refresh();

    public void refresh_safe(){
        if(world != null && world.isAreaLoaded(pos,15)){
            this.refresh();
        }
    }

    public void executeTask(){
        this.refresh_safe();
    }

    public boolean isStillValid(){
        return true;
    }

    @Override
    public OccupiedArea getOccupiedArea() {
        if(this.isWorkValid()) return occupiedArea;
        return OccupiedArea.EMPTY;
    }

    public static OccupiedArea getOccupiedArea(CompoundNBT nbt){
        return OccupiedArea.fromNBT(nbt.getCompound("occupiedArea"));
    }

    public static OccupiedArea getOccupiedArea(TownWorkerData data){
        return getOccupiedArea(data.getWorkData());
    }

    @Override
    public boolean isWorkValid(){
        if(workerState==NOT_INITIALIZED) this.refresh_safe();
        return workerState.isValid();
    }

    /**
     * doesn't refresh the tile entity if didn't initialize
     */
    public boolean isValid(){
        return this.workerState.isValid();
    }

    public static boolean isValid(CompoundNBT nbt){
        return TownWorkerState.fromByte(nbt.getByte("workerState")).isValid();
    }

    public static boolean isValid(TownWorkerData data){
        return isValid(data.getWorkData());
    }

    public boolean isOccupiedAreaOverlapped(){
        return this.workerState == TownWorkerState.OCCUPIED_AREA_OVERLAPPED;}

    protected CompoundNBT getBasicWorkData(){
        CompoundNBT nbt = new CompoundNBT();
        nbt.putLong("pos", this.pos.toLong());
        nbt.putByte("workerState", workerState.getStateNum());
        if(this.occupiedArea != null) nbt.put("occupiedArea", occupiedArea.toNBT());
        return nbt;
    }
    protected void setBasicWorkData(CompoundNBT data){
        workerState = TownWorkerState.fromByte(data.getByte("workerState"));
        occupiedArea = OccupiedArea.fromNBT(data.getCompound("occupiedArea"));
    }

    public void setWorkerState(TownWorkerState workerState) {
        this.workerState = workerState;
    }
    public TownWorkerState getWorkerState() {
        return workerState;
    }

    public static TownWorkerState getWorkerState(CompoundNBT nbt){
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
    public void readCustomNBT(CompoundNBT compoundNBT, boolean b) {}
    @Override
    public void writeCustomNBT(CompoundNBT compoundNBT, boolean b) {}
}
