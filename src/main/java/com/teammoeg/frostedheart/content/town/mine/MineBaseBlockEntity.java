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

package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerStatus;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.house.HouseBlockEntity;
import com.teammoeg.frostedheart.content.town.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.blockscanner.FloorBlockScanner;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.Math.exp;

public class MineBaseBlockEntity extends AbstractTownWorkerBlockEntity<MineBaseState> {
    public Set<BlockPos> linkedMines = new HashSet<>();
    private int volume;
    private int area;
    private int rack;
    private int chest;
    private double temperature;
    private double rating;

    public MineBaseBlockEntity(BlockPos pos, BlockState state){
        super(FHBlockEntityTypes.MINE_BASE.get(),pos,state);
    }

    public boolean isStructureValid(MineBaseState state){
        BlockPos mineBasePos = this.getBlockPos();
        BlockPos doorPos = BlockScanner.getDoorAdjacent(level, mineBasePos);
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
            MineBaseBlockScanner scanner = new MineBaseBlockScanner(level, startPos);
            if(scanner.scan()){
                this.area = scanner.getArea();
                this.volume = scanner.getVolume();
                this.rack = scanner.getRack();
                this.chest = scanner.getChest();
                this.linkedMines = scanner.getLinkedMines();
                state.setOccupiedArea(scanner.getOccupiedArea());
                this.temperature = scanner.getTemperature();
                return true;
            }
        }
        return false;
    }

    public double computeRating() {
        double rackRating = 1 - exp(-this.rack);
        double chestRating = 1 - exp(-this.chest * 0.4);
        double spaceRating = HouseBlockEntity.calculateSpaceRating(this.volume, this.area);
        double temperatureRating = HouseBlockEntity.calculateTemperatureRating(this.temperature);
        return this.rating = spaceRating*0.15 + temperatureRating*0.15 + chestRating*0.35 + rackRating*0.35;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.MINE_BASE;
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

    public double getRating(){
        if(isWorkValid()) {
            if (this.rating == 0) return this.computeRating();
            return this.rating;
        }
        return 0;
    }
    public int getVolume(){
        return this.isWorkValid()?this.volume:0;
    }
    public int getArea() {
        return this.isWorkValid() ? this.area : 0;
    }
    public int getRack() {
        return this.isWorkValid() ? this.rack : 0;
    }
    public int getChest() {
        return this.isWorkValid() ? this.chest : 0;
    }
    public double getTemperature() {
        return this.isWorkValid() ? this.temperature : 0;
    }


    //由于需要寻找Mine，MineBase可能需要扫描更远的位置，因此设置需要加载的区块更远。
    @Override
    public void refresh_safe(){
        if(level != null && level.isAreaLoaded(this.worldPosition,63)){
            super.refresh_safe();
        }
    }

    public void refresh(MineBaseState state) {
    	state.biomePath=CRegistryHelper.getBiomeKeyRuntime(level, CUtils.fastGetBiome(level, worldPosition).get());
    	if(this.isStructureValid(state)) {
	        if(!this.isOccupiedAreaOverlapped()){
	            state.status=TownWorkerStatus.VALID ;
	            return;
	        }
    	}else {
    		state.status=TownWorkerStatus.NOT_VALID_STRUCTURE;
    	}
    }

    @Override
    public boolean isStillValid() {
        return this.isWorkValid();
    }
}
