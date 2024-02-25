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

import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.town.ITownBlockTE;
import com.teammoeg.frostedheart.town.TownWorkerType;
import com.teammoeg.frostedheart.town.resident.Resident;
import com.teammoeg.frostedheart.util.BlockScanner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * A house in the town.
 */
public class HouseTileEntity extends TileEntity implements ITownBlockTE {

    public static final double COMFORTABLE_TEMP = 25;
    public static final int OPTIMAL_VOLUME = 100;
    public static final int BEST_DECORATION = 100;

    public int size; // how many resident can live here
    public List<Resident> residents;
    public double temperature;
    public int volume;
    public int decoration;

    public HouseTileEntity() {
        super(FHTileTypes.HOUSE.get());
    }

    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Get the rating based on house status
     * TODO: this is now a very crude numerical experiment.
     *
     * @return a rating in range of zero to one
     */
    public double getRating() {
        double tempDiff = Math.abs(temperature - COMFORTABLE_TEMP);
        double tempRating = 1 - Math.min(1.0, tempDiff / COMFORTABLE_TEMP);
        double decoRating = (double) decoration / BEST_DECORATION;
        double volumeRating = (double) volume / OPTIMAL_VOLUME;
        return (decoRating + volumeRating + tempRating) / 3;
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT data = new CompoundNBT();
        // TODO: Serialize resident
        data.putInt("size", size);
        data.putDouble("temperature", temperature);
        data.putInt("volume", volume);
        data.putInt("decoration", decoration);
        return data;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.HOUSE;
    }

    /**
     * Check room insulation
     * Check minimum volume
     * Check within generator range (or just check steam connection instead?)
     *
     * @param housePos the core block
     * @return whether this room is well-defined
     */
    private boolean isRoomValid(BlockPos housePos) {
        // TODO
        return false;
    }

    /**
     * Check if work environment is valid
     */
    @Override
    public boolean isWorkValid() {
        BlockPos pos = this.getPos();
        boolean roomValid = isRoomValid(pos);
        boolean tempConstraint = temperature >= 0 && temperature <= 50;
        return roomValid && tempConstraint;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        size = data.getInt("size");
        temperature = data.getDouble("temperature");
        volume = data.getInt("volume");
        decoration = data.getInt("decoration");
    }
}


class HouseBlockScanner extends BlockScanner{
    int area = 0;
    int volume = 0;
    Map<String/*block.getName()*/, Integer> decorations = new HashMap<>();
    double decorationScore = 0;
    boolean isValid = true;
    double temperature = 0;//average temperature
    Set<AbstractMap.SimpleEntry<Integer, Integer>> occupiedArea = new HashSet<>();

    HouseBlockScanner(World world, BlockPos startPos){
        super(world, startPos);
    }

    boolean isFloorBlock(BlockPos pos){
        BlockState blockState = world.getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
    }

    boolean isWallBlock(BlockPos pos){
        BlockState blockState = world.getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.DOORS) || blockState.isIn(BlockTags.WALLS));
    }

    boolean isHouseBlock(BlockPos pos){
        return isFloorBlock(pos) || isWallBlock(pos);
    }

    boolean isValidFloor(BlockPos pos){
        if(!isFloorBlock(pos)) return false;
        AbstractMap.SimpleEntry<Integer, Boolean> information = countBlocksAbove(pos, this::isHouseBlock);
        if(!information.getValue()){
            this.isValid = false;
            return false;//判断是否有露天方块
        }else return information.getKey() >= 2;//判断距离房顶空间是否大于等于2
    }

    void addDecoration(BlockPos pos){
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if( blockState.isIn(FHTags.Blocks.DECORATIONS) || Objects.requireNonNull(block.getRegistryName()).getNamespace().equals("cfm")) {
            String name = block.toString();
            if(decorations.get(name) == null || decorations.get(name) == 0){
                decorations.put(name, 1);
            }else{
                decorations.put(name, decorations.get(name) + 1);
            }
        }
    }


    public HashSet<BlockPos> nextScanningBlocks(BlockPos startPos){
        HashSet<BlockPos> possibleFloors = getPossibleFloor(startPos);
        for(BlockPos blockPos : possibleFloors){
            if(world.getBlockState(blockPos).isIn(BlockTags.CLIMBABLE)){
                for(BlockPos ladder : getBlocksAboveAndBelow(blockPos, (pos)->!(world.getBlockState(pos).isIn(BlockTags.CLIMBABLE)) )){
                    possibleFloors.addAll(getPossibleFloor(ladder));
                }
            }
        }
        for(BlockPos possibleBlock : possibleFloors){
            if(scannedBlocks.contains(possibleBlock.toLong())) {
                possibleFloors.remove(possibleBlock);
                continue;
            }
            if(!isValidFloor(possibleBlock)) {
                possibleFloors.remove(possibleBlock);
                scannedBlocks.add(possibleBlock.toLong());
            }
        }
        return possibleFloors;
    }

    boolean scan(){
        //第一次扫描，确定地板的位置，并判断是否有露天的地板
        this.scan(512, (scanner, pos)->{
            area++;
            occupiedArea.add(new AbstractMap.SimpleEntry<>(pos.getX(), pos.getZ()));
        }, (pos)->this.isValid);
        if(!this.isValid) return false;
        BlockScanner airScanner = new BlockScanner(world, startPos.up());
        airScanner.scan(4096, (pos)->{

        }, (pos)->BlockScanner.getBlocksAdjacent(pos, (pos1)->world.getBlockState(pos1).isAir()),
                (useless)->!this.isValid);
        int scanTimes = 0;
        while(!scanningBlocks.isEmpty() && isValid){
            if(scanTimes > 512){
                isValid = false;
                return false;
            }
            HashSet<BlockPos> scanningBlocksNew = new HashSet<>();

            scanningBlocks = scanningBlocksNew;
            scanTimes++;
        }
        return true;
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