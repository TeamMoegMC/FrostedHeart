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
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MineBlockEntity extends AbstractTownBuildingBlockEntity<MineBuilding> implements MenuProvider {

    public MineBlockEntity(BlockPos pos, BlockState state){
        super(FHBlockEntityTypes.MINE.get(),pos,state);
    }

    public boolean scanStructure(MineBuilding building){
        return true;
//        MineBlockScanner scanner = new MineBlockScanner(level, this.getBlockPos().above(), 512);
//        if(scanner.scan()){
//            double validStoneOrOre = scanner.getValidStone();
//            building.setOccupiedVolume(scanner.getOccupiedVolume());
//            return validStoneOrOre > 16;
//        }
//        return false;
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
        super.refresh(building);
        Holder<Biome> biomeHolder = CUtils.fastGetBiome(level, worldPosition);
        building.setBiomePath(CRegistryHelper.getBiomeKeyRuntime(level, biomeHolder.value()));
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

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new MineMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.mine");
    }
}
