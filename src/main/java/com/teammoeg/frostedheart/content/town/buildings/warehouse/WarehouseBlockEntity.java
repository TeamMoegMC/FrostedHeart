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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.decoration.WarehouseStorageRackBlock;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class WarehouseBlockEntity extends AbstractTownBuildingBlockEntity<WarehouseBuilding> implements MenuProvider {

    public WarehouseBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WAREHOUSE.get(),pos,state);
    }

    @Override
    public void refresh(@NotNull WarehouseBuilding building) {
        super.refresh(building);
        ITownWithBuildings buildingTown = this.getTown();
        if(buildingTown instanceof TeamTown teamTown){
            teamTown.getTownData().ifPresent(TeamTownData::reloadMaxCapacity);
        }

    }

    public boolean scanStructure(WarehouseBuilding building){
		BlockPos housePos = this.getBlockPos();
		RoomData rd=BlockScanner.scanRoomDataFromBlock(level,housePos);
		if (rd!=null&&rd.doors.size()>0) {
        	building.setArea(rd.area);
        	building.setVolume(rd.volume);
            //容量与体积相似，但是在随着房间高度增高略有衰减
            building.setDecorationAmount(rd.countInsideBlock(t->t.getBlock() instanceof WarehouseStorageRackBlock));

            building.setCapacity(building.getArea() * Math.pow(building.getVolume() * 0.02 / building.getArea(), 0.9) * 1980 + building.getDecorationAmount() * 512);
            building.setOccupiedVolume(rd.calculateOccupiedVolume());
            return true;
            
        }
        return false;
    }


    @Override
    public @Nullable WarehouseBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
        if(abstractTownBuilding instanceof WarehouseBuilding){
            return (WarehouseBuilding) abstractTownBuilding;
        }
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new WarehouseMenu(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.warehouse");
    }

    @Override
    public @NotNull WarehouseBuilding createBuilding() {
        return new WarehouseBuilding(this.getBlockPos());
    }
}
