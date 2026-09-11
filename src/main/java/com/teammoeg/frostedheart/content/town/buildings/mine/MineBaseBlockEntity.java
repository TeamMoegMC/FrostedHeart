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
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MineBaseBlockEntity extends AbstractTownBuildingBlockEntity<MineBaseBuilding> implements MenuProvider {

    public MineBaseBlockEntity(BlockPos pos, BlockState state){
        super(FHBlockEntityTypes.MINE_BASE.get(),pos,state);
    }

    public boolean scanStructure(MineBaseBuilding building){
		BlockPos housePos = this.getBlockPos();
		RoomData rd=BlockScanner.scanRoomDataFromBlock(level,housePos);
		if (rd!=null&&rd.doors.size()>0) {
            building.setArea(rd.area);
            building.setVolume(rd.volume);
            //this.rack = scanner.getRack();
            //this.chest = scanner.getChest();
            building.setOccupiedVolume(rd.calculateOccupiedVolume());
            FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
            double effectiveFloorBlocks = TownMathFunctions.calculateSpaceRating(
            	rd.volume,
                    rd.area,
                    scoring.spaceAreaCoefficient.get(),
                    scoring.spaceHeightLogCoefficient.get(),
                    scoring.spaceHeightLogOffset.get(),
                    scoring.spaceResponseScale.get(),
                    scoring.spaceResponseExponent.get())
                    * rd.area;
            int calculated = (int) (effectiveFloorBlocks
                    / FHConfig.SERVER.TOWN.MINING.floorBlocksPerWorkerSlot.get());
            building.setMaxResidents(Math.max(
                    FHConfig.SERVER.TOWN.MINING.minimumWorkerSlots.get(),
                    calculated
            ));
            return true;
        }
        return false;
    }

/*
    @Override
    public CompoundTag getWorkData() {
        CompoundTag nbt = getBasicWorkData();
        if(this.isValid()) {
            ListTag list = new ListTag();
            for (BlockPos pos : this.linkedMines) {
                list.add(LongTag.valueOf(pos.asLong()));
            }
            nbt.put("linkedMines", list);
            nbt.putDouble("rating", this.rating);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundTag data) {
        setBasicWorkData(data);
    }*/


    @Override
    public @Nullable MineBaseBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
        if(abstractTownBuilding instanceof MineBaseBuilding){
            return (MineBaseBuilding) abstractTownBuilding;
        }
        return null;
    }

    @Override
    public @NotNull MineBaseBuilding createBuilding() {
        return new MineBaseBuilding(this.getBlockPos());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new MineBaseMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.mine_base");
    }
}
