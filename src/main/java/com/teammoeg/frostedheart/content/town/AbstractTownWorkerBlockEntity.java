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

import java.util.UUID;

import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.scheduler.ScheduledTaskTileEntity;
import com.teammoeg.chorda.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractTownWorkerBlockEntity<T extends WorkerState> extends CBlockEntity implements
        TownBlockEntity<T>, ScheduledTaskTileEntity, CBlockInterfaces.IActiveState, CTickableBlockEntity,IOwnerTile {
    
    protected boolean addedToSchedulerQueue = false;
    private UUID owner;
    public AbstractTownWorkerBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state)  {
        super(type,pos,state);
    }

    @Override
	public UUID getStoredOwner() {
		return owner;
	}

	@Override
	public void setStoredOwner(UUID id) {
		owner=id;
	}

	/**
     * 重新进行结构的扫描、温度的检测等工作，并刷新方块的状态
     */
    public abstract void refresh(T state);

    public void refresh_safe(){
    	//System.out.println(getState());
        if(level != null && level.isLoaded(worldPosition)){
        	T state=getState();
        	if(state!=null)
        		this.refresh(state);
        }
    }
    public TeamTownData getTownData() {
    	TeamDataHolder datatype=CTeamDataManager.getDataByResearchID(owner);
    	if(datatype==null)return null;
    	return datatype.getData(FHSpecialDataTypes.TOWN_DATA);
    }
    public T getState() {
    	TeamTownData teamTownData=getTownData();
    	if(teamTownData==null){
            return null;
        }
        TownWorkerData blockData = teamTownData.blocks.get(worldPosition);
        if(blockData.getType() == this.getWorkerType()){
            return (T) blockData.getState();
        } else{
            FHMain.LOGGER.error("AbstractTownWorkerBlockEntity : Trying to get WorkerState, but type of TownWorkerData [{}] is not same with TownBlockEntity type [{}]", blockData.getType(), this.getWorkerType());
            FHMain.LOGGER.info("Trying to fix worker type wrong in TeamTownData");
            teamTownData.blocks.put(worldPosition, new TownWorkerData(this.getWorkerType(), worldPosition, this.getPriority()));
        }
    	return (T) teamTownData.getState(this.worldPosition);
    }
    public void executeTask(){
        this.refresh_safe();
    }

    public boolean isStillValid(){
        return true;
    }

    @Override
    public OccupiedArea getOccupiedArea() {
        if(this.isWorkValid()) {
        	return getState().getOccupiedArea();
        }
        return OccupiedArea.EMPTY;
    }

    public static OccupiedArea getOccupiedArea(TownWorkerData data){

        return data.getState().getOccupiedArea();
    }
    public TownWorkerStatus getStatus() {
    	T state=getState();
    	if(state==null)return TownWorkerStatus.NOT_VALID;
    	return state.status;
    }
    @Override
    public boolean isWorkValid(){
    	TownWorkerStatus status=getStatus();
        if(status== TownWorkerStatus.NOT_INITIALIZED) this.refresh_safe();
        return status.isValid();
    }


    public boolean isValid(){
    	TownWorkerStatus status=getStatus();
        return status.isValid();
    }

    public boolean isOccupiedAreaOverlapped(){
        return getStatus() == TownWorkerStatus.OCCUPIED_AREA_OVERLAPPED;
    }

    public abstract boolean isStructureValid(T state);

    public static TownWorkerStatus getStatus(TownWorkerData data){
        return data.getState().status;
    }

    @Override
	public void setStatus(TownWorkerStatus status) {
		T state=getState();
		if(state!=null)
			state.status=status;
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
    public void readCustomNBT(CompoundTag compoundNBT, boolean b) {
    	if(compoundNBT.contains("owner"))
    		owner=UUID.fromString(compoundNBT.getString("owner"));
    }
    @Override
    public void writeCustomNBT(CompoundTag compoundNBT, boolean b) {
    	
    	if(owner!=null)
    		compoundNBT.putString("owner", owner.toString());
    }
}
