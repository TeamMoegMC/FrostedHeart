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

package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.chorda.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.blockscanner.FloorBlockScanner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class WarehouseBlockEntity extends AbstractTownWorkerBlockEntity<WareHouseState> implements MenuProvider {

    private boolean addedToSchedulerQueue = false;

    public WarehouseBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WAREHOUSE.get(),pos,state);
    }

    public boolean isStructureValid(WareHouseState state){
        BlockPos warehousePos = this.getBlockPos();
        BlockPos doorPos = BlockScanner.getDoorAdjacent(level, warehousePos);
        if (doorPos == null) return false;
        BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos)->!(Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
        for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
            assert floorBelowDoor != null;
            BlockPos startPos = floorBelowDoor.relative(direction);//找到门下方块旁边的方块
            if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos.below()) || FloorBlockScanner.isHouseBlock(level, startPos.above(2))) {//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                    continue;
                }
                startPos = startPos.below();
            }
            WarehouseBlockScanner scanner = new WarehouseBlockScanner(level, startPos);
            if(scanner.scan()){
            	state.area = scanner.getArea();
            	state.volume = scanner.getVolume();
                //容量与体积相似，但是在随着房间高度增高略有衰减
            	state.capacity = state.area*Math.pow((state.volume*0.02/state.area), 0.9)*37;
            	state.setOccupiedArea(scanner.getOccupiedArea());
                return true;
            }
        }
        return false;
    }

    public void refresh(WareHouseState state){
    	if(this.isStructureValid(state)) {
	        if(!this.isOccupiedAreaOverlapped()){
	        	state.status = TownWorkerStatus.VALID;
	        }
    	}else {
    		state.status =TownWorkerStatus.NOT_VALID_STRUCTURE;
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.WAREHOUSE;
    }



    public int getVolume(){
        return this.isWorkValid()?getState().volume:0;
    }
    public int getArea(){
        return this.isWorkValid()?getState().area:0;
    }
    public double getCapacity(){
        return this.isWorkValid()?getState().capacity:0;
    }

    @Override
    public void tick() {
        if(!this.addedToSchedulerQueue){
            SchedulerQueue.add(this);
            this.addedToSchedulerQueue = true;
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WarehouseMenu(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.warehouse");
    }
}
