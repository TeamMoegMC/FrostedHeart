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

package com.teammoeg.frostedheart.town.house;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.town.ITownBlockTE;
import com.teammoeg.frostedheart.town.TownWorkerType;
import com.teammoeg.frostedheart.town.resident.Resident;
import com.teammoeg.frostedheart.util.BlockScanner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.teammoeg.frostedheart.town.house.HouseBlockScanner.isHouseBlock;

/**
 * A house in the town.
 */
public class HouseTileEntity extends TileEntity implements ITownBlockTE {

    public static final double COMFORTABLE_TEMP = 24;
    //public static final int OPTIMAL_VOLUME = 100;
    //public static final int BEST_DECORATION = 100;

    public int size; // how many resident can live here
    public int maxResidents;
    public List<Resident> residents;
    public double temperature;
    public int volume;
    public int decoration;
    public int area;
    private Map<String, Integer> decorations;
    public Set<AbstractMap.SimpleEntry<Integer, Integer>> occupiedArea;

    public HouseTileEntity() {
        super(FHTileTypes.HOUSE.get());
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.HOUSE;
    }

    /**
     * Check if work environment is valid.
     * <p>
     * For the house, this implies whether the house would accommodate the residents,
     * consume resources, and other.
     * <p>
     * Room structure should be valid.
     * Temperature should be within a reasonable range.
     */
    @Override
    public boolean isWorkValid() {
        BlockPos pos = this.getPos();
        boolean roomValid = isStructureValid(pos);
        boolean tempConstraint = temperature >= 0 && temperature <= 50;
        return roomValid && tempConstraint;
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT data = new CompoundNBT();
        for (int i = 0; i < residents.size(); i++) {
            data.put("resident" + i, residents.get(i).serialize());
        }
        data.putInt("size", size);
        data.putDouble("temperature", temperature);
        data.putInt("volume", volume);
        data.putInt("decoration", decoration);
        return data;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        residents = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Resident resident = new Resident();
            resident.deserialize(data.getCompound("resident" + i));
            residents.add(resident);
        }
        size = data.getInt("size");
        temperature = data.getDouble("temperature");
        volume = data.getInt("volume");
        decoration = data.getInt("decoration");
    }

    /**
     * Determine whether the house structure is well-defined.
     * <p>
     * Check room insulation
     * Check minimum volume
     * Check within generator range (or just check steam connection instead?)
     * <p>
     *
     * @param housePos the House block
     * @return whether the house structure is valid
     */
    private boolean isStructureValid(BlockPos housePos) {
        Set<BlockPos> doorPosSet = BlockScanner.getBlocksAdjacent(housePos, (pos) -> Objects.requireNonNull(world).getBlockState(pos).isIn(BlockTags.DOORS));
        if (doorPosSet.isEmpty()) return false;
        for (BlockPos doorPos : doorPosSet) {
            BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos)->!(Objects.requireNonNull(world).getBlockState(pos).isIn(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
            for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
                //FHMain.LOGGER.debug("HouseScanner: creating new HouseBlockScanner");
                assert floorBelowDoor != null;
                BlockPos startPos = floorBelowDoor.offset(direction);//找到门下方块旁边的方块
                //FHMain.LOGGER.debug("HouseScanner: start pos 1" + startPos);
                if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                    if(!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos.down()) || isHouseBlock(world, startPos.up(2))){//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                        continue;
                    }
                    startPos = startPos.down();
                    //FHMain.LOGGER.debug("HouseScanner: start pos 2" + startPos);
                }
                HouseBlockScanner scanner = new HouseBlockScanner(this.world, startPos);
                if (scanner.check()) {
                    //FHMain.LOGGER.debug("HouseScanner: scan successful");
                    this.volume = scanner.getVolume();
                    this.area = scanner.getArea();
                    this.decorations = scanner.getDecorations();
                    this.temperature = scanner.getTemperature();
                    this.occupiedArea = scanner.getOccupiedArea();
                    //FHMain.LOGGER.debug("HouseScanner:\n volume: " + volume + " area: " + area + " decorations: ");
                    //for(String s : decorations.keySet()) FHMain.LOGGER.debug("HouseScanner.decoration: " + s);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get a comfort rating based on how the house is built.
     * <p>
     * This would affect the mood of the residents on the next day.
     *
     * @return a rating in range of zero to one
     */
    public double getRating() {
        return (calculateDecorationRating(this.decorations, this.area)
                + calculateSpaceRating(this.volume, this.area)
                + calculateTemperatureRating(this.temperature)) / 3;
    }

    private static double calculateTemperatureRating(double temperature) {
        double tempDiff = Math.abs(COMFORTABLE_TEMP - temperature);
        return 0.017 + 1 / (1 + Math.exp(0.4 * (tempDiff - 10)));
    }

    private static double calculateDecorationRating(Map<?, Integer> decorations, int area) {
        double score = 0;
        for (Integer num : decorations.values()) {
            score += Math.log(num + 0.32) * 1.75 + 0.9;
            //log(x+0.72)*1.5+0.5, sqrt(x*0.8)*1.5
        }
        return Math.min(1, score / (6 + area / 16.0f));
    }

    private static double calculateSpaceRating(int volume, int area) {
        double height = volume / (float) area;
        double score = area * (1.55 + Math.log(height - 1.6) * 0.6);
        return 1 - Math.exp(-0.024 * Math.pow(score, 1.11));
    }
}


/*
            for(BlockPos scanningBlock : scanningBlocks){
                AbstractMap.SimpleEntry<Integer, Boolean> floorBlockInformation = countBlocksAbove(possibleBlock, (pos)->{
                    boolean stopAt = this.isHouseBlock(pos);
                    addDecoration(pos);
                    if(!stopAt) {
                        temperature+= ChunkHeatData.getTemperature(world, pos);
                    }
                    return stopAt;//判断方块是否是天花板，在此过程中顺便帮忙计算一下平均温度和装饰物
                });
                scanningBlocksNew.addAll(nextScanningBlocks.apply(this, scanningBlock));
                scannedBlocks.add(scanningBlock.toLong());
            }
 */