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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockEntity;
import com.teammoeg.frostedheart.content.town.ChunkTownResourceCapability;
import com.teammoeg.frostedheart.content.town.TownWorkerStatus;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockEntity;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class MineBlockEntity extends AbstractTownWorkerBlockEntity<MineState> {
    private int avgLightLevel;
    private int validStoneOrOre;
    private double temperature;
    private double rating;
    /**
     * 方块所在区块的生物群系
     */
    private ResourceLocation biome;
    private double chunkResourceReserves;
    /**
     * 缓存工作时消耗的区块矿产资源，在方块所在区块加载时将消耗量加到{@link ChunkTownResourceCapability}中。
     */
    private double chunkResourceReservesCost;
    /**
     * 上次向区块中更新资源储量时的工作ID，详见{@link MineWorker}中的WORK_ID
     */
    private long lastSyncedWorkID;
    /**
     * 最新从城镇数据中获取的工作ID
     */
    private long latestWorkID;

    public MineBlockEntity(BlockPos pos, BlockState state){
        super(FHBlockEntityTypes.MINE.get(),pos,state);
    }

    public boolean isStructureValid(MineState state){
        MineBlockScanner scanner = new MineBlockScanner(level, this.getBlockPos().above());
        if(scanner.scan()){
            this.avgLightLevel = scanner.getLight();
            this.validStoneOrOre = scanner.getValidStone();
            state.setOccupiedArea(scanner.getOccupiedArea());
            this.temperature = scanner.getTemperature();
            return validStoneOrOre > 0;
        }
        return false;
    }

    public double getRating() {
        if(this.isWorkValid()) return this.rating;
        else return 0;
    }
    public int getAvgLightLevel() {
        if(this.isWorkValid()) return this.avgLightLevel;
        else return 0;
    }
    public int getValidStoneOrOre() {
        if(this.isWorkValid()) return this.validStoneOrOre;
        else return 0;
    }

    public void computeRating(){
        double lightRating = 1 - Math.exp(-this.avgLightLevel);
        double stoneRating = Math.min(this.validStoneOrOre / 255.0F, 1);
        double temperatureRating = HouseBlockEntity.calculateTemperatureRating(this.temperature);
        this.rating = (lightRating * 0.3 + stoneRating * 0.3 + temperatureRating * 0.4) /* * (1 + 4 * this.linkedBaseRating)*/;
    }


    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.MINE_BASE;
    }

    /*@Override
    public CompoundTag getWorkData() {
        CompoundTag nbt = getBasicWorkData();
        if(this.isValid()){
            nbt.putDouble("rating", this.rating);
            nbt.putDouble("chunkResourceReserves", this.chunkResourceReserves);
            nbt.putString("biome", this.biome.toString());
        }
        this.updateResourceReserves();
        nbt.putLong("lastSyncedWorkID", this.lastSyncedWorkID);
        return nbt;
    }

    @Override
    public void setWorkData(CompoundTag data) {
        this.setBasicWorkData(data);
        long latestWorkID = data.getLong("latestWorkID");
        if(this.latestWorkID != latestWorkID){
            this.latestWorkID = latestWorkID;
            this.chunkResourceReserves = data.getDouble("chunkResourceReserves");
        }
    }*/


    public void refresh(MineState state) {
        if(this.isOccupiedAreaOverlapped()){
            this.isStructureValid(state);
            return;
        }
        state.status = isStructureValid(state) ? TownWorkerStatus.VALID : TownWorkerStatus.NOT_VALID;
        if(this.isValid()) {
            assert this.level != null;
            this.computeRating();
            this.biome = level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getKey(level.getBiome(this.getBlockPos()).value());
        }
    }

    public static void setLinkedBase(TownWorkerData mineData, BlockPos pos){
        if(mineData == null) return;
        if(pos == null) return;
        ((MineState)mineData.getState()).setConnectedBase(pos);
    }

    /**
     * 更新区块中的资源储量，并重新从区块中获取
     */
    public void updateResourceReserves(){
        if(this.level == null) return;
        if(!this.level.isLoaded(this.getBlockPos())) return;
        if(this.chunkResourceReservesCost <= 0 && this.lastSyncedWorkID == this.latestWorkID && !this.isValid()){
            return;
        }

        this.level.getChunkAt(this.getBlockPos()).getCapability(FHCapabilities.CHUNK_TOWN_RESOURCE.capability(), null).ifPresent(
                capability -> {
                    if(this.chunkResourceReservesCost > 0 && this.latestWorkID != this.lastSyncedWorkID){//将缓存的资源消耗量同步到区块数据中
                        capability.costReserves(ChunkTownResourceCapability.ChunkTownResourceType.ORE , this.chunkResourceReservesCost);
                        this.chunkResourceReservesCost = 0;
                        this.lastSyncedWorkID = this.latestWorkID;
                    }

                    if(this.isValid()){//从区块中获取当前的资源量
                        this.chunkResourceReserves = capability.getOrGenerateReserves(ChunkTownResourceCapability.ChunkTownResourceType.ORE);
                    }
                }
        );
    }

    //由于方块放置后生物群系理论上不会再变化，仅在第一次放置时读取一次生物群系，之后都直接保存在方块实体里
    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        this.biome = nbt.contains("biome") ? new ResourceLocation(nbt.getString("biome")) : null;
        this.lastSyncedWorkID = nbt.getLong("lastSyncedWorkID");
        this.latestWorkID = nbt.getLong("latestWorkID");
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        if(this.biome != null){
            nbt.putString("biome", biome.toString());
        }
        nbt.putLong("lastSyncedWorkID", this.lastSyncedWorkID);
        nbt.putLong("latestWorkID", this.latestWorkID);
    }
}
