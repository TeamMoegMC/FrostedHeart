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

package com.teammoeg.frostedheart.content.town.block;

import java.util.Optional;
import java.util.UUID;

import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.scheduler.ScheduledTaskTileEntity;
import com.teammoeg.chorda.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractTownBuildingBlockEntity<T extends AbstractTownBuilding> extends CBlockEntity implements
        TownBlockEntity<T>, ScheduledTaskTileEntity, CBlockInterfaces.IActiveState, CTickableBlockEntity {
    
    protected boolean addedToSchedulerQueue = false;
    public ITownProviderSerializable<? extends ITownWithBuildings> townProvider;

    public AbstractTownBuildingBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state)  {
        super(type,pos,state);
    }

	/**
     * 重新进行结构的扫描、温度的检测等工作，并刷新城镇中的建筑状态
     */
    public void refresh(@NotNull T building){
        building.isStructureValid = scanStructure(building);
    };

    /**
     * 和refresh差不多，但是进行了一些检查
     */
    public void refresh_safe(T building){
        if(level != null && level.isLoaded(worldPosition) && building != null){
            this.refresh(building);
        }
    }

    public void executeTask(){
        this.getBuilding().ifPresent(this::refresh_safe);
    }

    public boolean isStillValid(){
        return true;
    }

    public Optional<T> getBuilding(){
        if(this.townProvider == null){
            return Optional.empty();
        }
        ITownWithBuildings town = townProvider.getTown();
        if(town == null){
            return Optional.empty();
        }
        Optional<AbstractTownBuilding> buildingOptional = town.getTownBuilding(this.worldPosition);
        if(buildingOptional.isPresent()){
            AbstractTownBuilding building = buildingOptional.get();
            if(building instanceof AbstractTownBuilding){
                return Optional.ofNullable(getBuilding(building));
            }
        }
        return Optional.empty();
    }

    public ITownWithBuildings getTown(){
        return townProvider.getTown();
    }


    public abstract boolean scanStructure(T building);


	public void addToSchedulerQueue(){
        if(!this.addedToSchedulerQueue){
            this.addedToSchedulerQueue = true;
            SchedulerQueue.add(this);
        }
    }

    public void tick(){
        this.addToSchedulerQueue();
    }

    @Override
    public void readCustomNBT(CompoundTag compoundNBT, boolean b) {
    	if(compoundNBT.contains("townProvider")){
            ITownProviderSerializable<? extends Town> providerRaw = ITownProviderSerializable.fromNBT(compoundNBT.getCompound("townProvider"));
            if(providerRaw != null){
                if(townProvider.getTownType().isAssignableFrom(ITownWithBuildings.class)){
                    //这里经过getTownType判断类型之后，townProvider的类型一定为ITownWithBuildings，所以应该不会有问题了
                    townProvider = (ITownProviderSerializable<? extends ITownWithBuildings>) providerRaw;
                }
            }
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag compoundNBT, boolean b) {
        if(townProvider != null){
            compoundNBT.put("townProvider", townProvider.serializeNBT());
        }
    }
}
