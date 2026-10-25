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

package com.teammoeg.frostedheart.content.town.block;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.scheduler.ScheduledTaskTileEntity;
import com.teammoeg.chorda.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.ITown;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.TownBuildingTypes;

public abstract class AbstractTownBuildingBlockEntity<T extends AbstractTownBuilding> extends CBlockEntity implements
        TownBlockEntity<T>, ScheduledTaskTileEntity, CBlockInterfaces.IActiveState, CTickableBlockEntity {
    
    public enum StructureScanResult {
        VALID,
        INVALID,
        DATA_UNAVAILABLE
    }

    private static final int ENVIRONMENT_REFRESH_TICKS = 20;
    private static final int STRUCTURE_REFRESH_TICKS = 200;
    private SchedulerQueue.Registration scanRegistration;
    private long nextStructureRefresh;
    private long nextEnvironmentRefresh;
    private long observedResourceGeneration = -1;
    private boolean lastScanCompleted;
    private AbstractTownBuilding placedBuilding;
    public ITownProviderSerializable<? extends ITownWithBuildings> townProvider;

    public boolean hasRegisteredPlacement(AbstractTownBuilding building) {
        return building != null && placedBuilding == building;
    }

    public void rememberPlacement(AbstractTownBuilding building) {
        placedBuilding = building;
    }

    public AbstractTownBuildingBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state)  {
        super(type,pos,state);
    }

	/**
     * 重新进行结构的扫描、温度的检测等工作，并刷新城镇中的建筑状态
     */
    public void refresh(@NotNull T building) {
        lastScanCompleted = false;
        building.updateBatch(() -> {
            StructureScanResult result = scanStructure(building);
            if (result == StructureScanResult.DATA_UNAVAILABLE) {
                return;
            }
            lastScanCompleted = true;
            building.setInitialized(true);
            building.setIsStructureValid(result == StructureScanResult.VALID);
            refreshEnvironment(building);
        });
    }

    protected void refreshEnvironment(T building) {
    }

    public void refresh_safe(T building) {
        if (!(level instanceof ServerLevel) || !isStillValid() || building == null) {
            return;
        }
        try {
            refresh(building);
        } catch (RuntimeException failure) {
            lastScanCompleted = false;
            FHMain.LOGGER.error("Town building refresh failed at {}", worldPosition, failure);
        }
        long now = level.getGameTime();
        nextStructureRefresh = now + (lastScanCompleted ? STRUCTURE_REFRESH_TICKS : ENVIRONMENT_REFRESH_TICKS);
        nextEnvironmentRefresh = now + ENVIRONMENT_REFRESH_TICKS;
    }

    @Override
    public void executeTask() {
        if (!(level instanceof ServerLevel serverLevel) || !isStillValid()) {
            return;
        }
        ITownWithBuildings town = getTown();
        if (town instanceof TeamTown playerTown) {
            playerTown.ensureCoreBound(serverLevel, worldPosition, this);
        }
        long resourceGeneration = TownBuildingTypes.resourceGeneration();
        if (resourceGeneration != observedResourceGeneration) {
            observedResourceGeneration = resourceGeneration;
            nextStructureRefresh = 0;
        }
        getBuilding().ifPresent(building -> {
            long now = level.getGameTime();
            if (now >= nextStructureRefresh) {
                refresh_safe(building);
            } else if (now >= nextEnvironmentRefresh) {
                building.updateBatch(() -> refreshEnvironment(building));
                nextEnvironmentRefresh = now + ENVIRONMENT_REFRESH_TICKS;
            }
        });
    }

    public boolean isStillValid(){
        return level != null
                && !isRemoved()
                && level.isLoaded(worldPosition)
                && level.getBlockEntity(worldPosition) == this;
    }

    /** Lookup is pure. Registration belongs to onLoad and explicit lifecycle work. */
    public Optional<T> getBuilding() {
        if (!isStillValid()) {
            return Optional.empty();
        }
        ITownWithBuildings town = getTown();
        if (town == null) {
            return Optional.empty();
        }
        if (town instanceof TeamTown playerTown && level instanceof ServerLevel serverLevel
                && !playerTown.isBuildingDimension(serverLevel)) {
            return Optional.empty();
        }
        return town.getTownBuilding(worldPosition).map(this::getBuilding);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        nextStructureRefresh = 0;
        nextEnvironmentRefresh = 0;
        addToSchedulerQueue();
    }

    @Override
    public void onUnloaded() {
        cancelScheduledScan();
        super.onUnloaded();
    }

    @Override
    public void onRemoved() {
        cancelScheduledScan();
        if (level instanceof ServerLevel serverLevel && townProvider != null) {
            ITownWithBuildings town = townProvider.getTown();
            if (town instanceof TeamTown playerTown) {
                playerTown.onCoreRemoved(serverLevel, worldPosition, this);
            }
        }
        super.onRemoved();
    }

    private void cancelScheduledScan() {
        if (scanRegistration != null) {
            scanRegistration.close();
            scanRegistration = null;
        }
    }

    @Nullable
    public ITownWithBuildings getTown(){
        if (townProvider == null) {
            return null;
        }
        if (level != null && level.isClientSide) {
            return TeamTownData.getClientTown().orElse(null);
        }
        return townProvider.getTown();
    }


    public abstract StructureScanResult scanStructure(T building);


    public void addToSchedulerQueue() {
        if (level instanceof ServerLevel && scanRegistration == null) {
            scanRegistration = SchedulerQueue.register(this);
        }
    }

    public void tick() {
        // World registration is performed by onLoad, never by the client tick.
    }

    @Override
    public void readCustomNBT(CompoundTag compoundNBT, boolean isPacket) {
        if (!compoundNBT.contains("townProvider")) {
            return;
        }
        ITownProviderSerializable<? extends ITown> provider =
                ITownProviderSerializable.fromNBT(compoundNBT.getCompound("townProvider"));
        if (provider != null && ITownWithBuildings.class.isAssignableFrom(provider.getTownType())) {
            townProvider = (ITownProviderSerializable<? extends ITownWithBuildings>) provider;
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag compoundNBT, boolean b) {
        if(townProvider != null){
            compoundNBT.put("townProvider", townProvider.toNBT());
        }
    }
}
