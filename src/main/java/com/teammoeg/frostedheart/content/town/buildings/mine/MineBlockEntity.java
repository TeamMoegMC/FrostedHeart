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

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MineBlockEntity extends AbstractTownBuildingBlockEntity<MineBuilding> {

    public MineBlockEntity(BlockPos pos, BlockState state){
        super(FHBlockEntityTypes.MINE.get(),pos,state);
    }

    public boolean scanStructure(MineBuilding building){
        MineBlockScanner scanner = new MineBlockScanner(level, this.getBlockPos().above());
        if(scanner.scan()){
            double validStoneOrOre = scanner.getValidStone();
            building.setOccupiedArea(scanner.getOccupiedArea());
            return validStoneOrOre > 16;
        }
        return false;
    }

    /*
    public void computeRating(){
        double lightRating = 1 - Math.exp(-this.avgLightLevel);
        double stoneRating = Math.min(this.validStoneOrOre / 255.0F, 1);
        double temperatureRating = TownMathFunctions.calculateTemperatureRating(this.temperature);
        this.rating = (lightRating * 0.3 + stoneRating * 0.3 + temperatureRating * 0.4) /* * (1 + 4 * this.linkedBaseRating)*/;
    //}


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


    public void refresh(@NotNull MineBuilding building) {
        assert level != null;
        scanStructure( building);
        building.biomePath= CRegistryHelper.getBiomeKeyRuntime(level, CUtils.fastGetBiome(level, worldPosition).get());
    }

    @Override
    public @Nullable MineBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
        if(abstractTownBuilding instanceof MineBuilding){
            return (MineBuilding) abstractTownBuilding;
        }
        return null;
    }

    @Override
    public @NotNull MineBuilding createBuilding() {
        return new MineBuilding(this.getBlockPos());
    }
}