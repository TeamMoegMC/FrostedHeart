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

package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.house.HouseBlockScanner;
import com.teammoeg.frostedheart.content.town.house.HouseBlockEntity;
import com.teammoeg.frostedheart.content.town.resource.TownResourceManager;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedheart.content.town.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.blockscanner.FloorBlockScanner;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import java.util.*;

import java.util.AbstractMap.SimpleEntry;

public class HuntingBaseBlockEntity extends AbstractTownWorkerBlockEntity {
    @Getter
    private double rating = 0;
    //get volume
    @Getter
    private int volume;
    //get area
    @Getter
    private int area;
    //get chest num
    @Getter
    private int chestNum;
    //get bed num
    @Getter
    private int bedNum;
    //get tanning rack num
    @Getter
    private int tanningRackNum;
    //get temperature
    @Getter
    private double temperature;
    private Map<String, Integer> decorations;
    HeatEndpoint endpoint = HeatEndpoint.consumer(99, 1);
    LazyOptional<HeatEndpoint> endpointCap = LazyOptional.of(()-> endpoint);
    private double temperatureModifier = 0;
    //get max resident
    @Getter
    private int maxResident;

    public HuntingBaseBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.HUNTING_BASE.get(),pos,state);
    }


    public boolean isStructureValid() {
        BlockPos housePos = this.getBlockPos();
        List<BlockPos> doorPosSet = BlockScanner.getBlocksAdjacent(housePos, (pos) -> Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS));
        if (doorPosSet.isEmpty()) return false;
        for (BlockPos doorPos : doorPosSet) {
            BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos)->!(Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
            for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
                assert floorBelowDoor != null;
                BlockPos startPos = floorBelowDoor.relative(direction);//找到门下方块旁边的方块
                if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                    if(!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos.below()) || FloorBlockScanner.isHouseBlock(level, startPos.above(2))){//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                        continue;
                    }
                    startPos = startPos.below();
                }
                HuntingBaseBlockScanner scanner = new HuntingBaseBlockScanner(this.level, startPos);
                if (scanner.scan()) {
                    this.volume = scanner.getVolume();
                    this.area = scanner.getArea();
                    this.decorations = scanner.getDecorations();
                    this.temperature = scanner.getTemperature();
                    this.occupiedArea = scanner.getOccupiedArea();
                    this.chestNum = scanner.getChestNum();
                    this.bedNum = scanner.getBedNum();
                    this.tanningRackNum = scanner.getTanningRackNum();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isTemperatureValid(){
        double effective = temperature + temperatureModifier;
        return effective >= HouseBlockEntity.MIN_TEMP_HOUSE && effective <= HouseBlockEntity.MAX_TEMP_HOUSE;
    }

    public double getTemperatureModifier() {
        return isWorkValid() ? this.temperatureModifier : 0;
    }

    public double getEffectiveTemperature() {
        return temperature + temperatureModifier;
    }

    private double computeRating() {
        if(this.isValid()){
            return (HouseBlockEntity.calculateSpaceRating(this.volume, this.area) * (2 + HouseBlockEntity.calculateDecorationRating(this.decorations, this.area))
                    + 2 * HouseBlockEntity.calculateTemperatureRating(this.temperature + this.temperatureModifier) +
                    (1-Math.exp(-this.maxResident - chestNum)) ) / 6;
        }
        else return 0;
    }

    private int calculateMaxResidents(){
        if(this.isValid()){
            return Math.min((int)(HouseBlockEntity.calculateSpaceRating(this.volume, this.area) / 16 * this.area), Math.min(this.tanningRackNum, this.bedNum));
        }
        else return 0;
    }

    @Override
    public void tick() {
        assert level != null;
        if (!level.isClientSide) {
            if (endpoint.tryDrainHeat(1)) {
                temperatureModifier = Math.max(endpoint.getTempLevel() * 10, HouseBlockEntity.COMFORTABLE_TEMP_HOUSE);
                if (setActive(true)) {
                    setChanged();
                }
            } else {
                temperatureModifier = 0;
                if (setActive(false)) {
                    setChanged();
                }
            }
        } else if (getIsActive()) {
            FHClientUtils.spawnSteamParticles(level, worldPosition);
        }
        this.addToSchedulerQueue();
    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if(capability== FHCapabilities.HEAT_EP.capability() && facing == Direction.NORTH) {
            return endpointCap.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void refresh() {
        if (this.isOccupiedAreaOverlapped()) {
            this.isStructureValid();
            this.isTemperatureValid();
        } else {
            this.workerState = this.isStructureValid() && this.isTemperatureValid() ? TownWorkerStatus.VALID : TownWorkerStatus.NOT_VALID;
            this.rating = this.computeRating();
            this.maxResident = this.calculateMaxResidents();
        }
    }


    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.HUNTING_BASE;
    }

    @Override
    public CompoundTag getWorkData() {
        CompoundTag nbt = getBasicWorkData();
        if(this.isValid()){
            nbt.putDouble("rating",this.rating);
            nbt.putInt("maxResident",this.maxResident);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundTag data) {
        this.setBasicWorkData(data);
    }

    @Override
	public void invalidateCaps() {
		endpointCap.invalidate();
		super.invalidateCaps();
	}
}
