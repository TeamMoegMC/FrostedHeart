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

import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.scheduler.ScheduledTaskTileEntity;
import com.teammoeg.chorda.scheduler.SchedulerQueue;

import com.teammoeg.frostedheart.content.town.blockscanner.ConfinedSpaceScanner;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractTownWorkerBlockEntity extends CBlockEntity implements
        TownBlockEntity, ScheduledTaskTileEntity, CBlockInterfaces.IActiveState {
    @Getter
    @Setter
    public TownWorkerState workerState = TownWorkerState.NOT_INITIALIZED;
    public OccupiedArea occupiedArea;
    protected boolean addedToSchedulerQueue = false;
    public AbstractTownWorkerBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state)  {
        super(type,pos,state);
    }

    /**
     * 重新进行结构的扫描、温度的检测等工作，并刷新方块的状态
     */
    public abstract void refresh();

    public void refresh_safe(){
        if(level != null && level.isAreaLoaded(worldPosition,15)){
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

    /**
     * @param nbt the work data of town tile entity
     * @return occupied area of worker
     */
    public static OccupiedArea getOccupiedArea(CompoundTag nbt){
        if(nbt.contains("occupiedArea")){
            return OccupiedArea.fromNBT(nbt.getCompound("occupiedArea"));
        }
        if(nbt.contains("tileEntity")){
            return getOccupiedArea(nbt.getCompound("tileEntity"));
        }
        return null;
    }

    public static OccupiedArea getOccupiedArea(TownWorkerData data){
        return getOccupiedArea(data.getWorkData().getCompound("tileEntity"));
    }

    @Override
    public boolean isWorkValid(){
        if(workerState== TownWorkerState.NOT_INITIALIZED) this.refresh_safe();
        return workerState.isValid();
    }


    public boolean isValid(){
        return this.workerState.isValid();
    }

    public static boolean isValid(CompoundTag nbt){
        if(nbt.contains("workerState")){
            return TownWorkerState.fromByte(nbt.getByte("workerState")).isValid();
        }
        if(nbt.contains("tileEntity")){
            return isValid(nbt.getCompound("tileEntity"));
        }
        return false;
    }

    public static boolean isValid(TownWorkerData data){
        return isValid(data.getWorkData());
    }

    public boolean isOccupiedAreaOverlapped(){
        return this.workerState == TownWorkerState.OCCUPIED_AREA_OVERLAPPED;}

    public boolean isStructureValid(){
        return true;
    }

    protected CompoundTag getBasicWorkData(){
        CompoundTag nbt = new CompoundTag();
        nbt.putByte("workerState", workerState.getStateNum());
        if(this.occupiedArea != null) nbt.put("occupiedArea", occupiedArea.toNBT());
        return nbt;
    }
    protected void setBasicWorkData(CompoundTag data){
        if(data.contains("isOverlapped")){
            if(data.getBoolean(TownWorkerData.KEY_IS_OVERLAPPED)){
                this.workerState = TownWorkerState.OCCUPIED_AREA_OVERLAPPED;
            } else{
                if(this.workerState == TownWorkerState.OCCUPIED_AREA_OVERLAPPED){
                    this.workerState = TownWorkerState.NOT_INITIALIZED;
                }
            }
        }
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
