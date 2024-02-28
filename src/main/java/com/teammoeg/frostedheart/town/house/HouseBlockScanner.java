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
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.BlockScanner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.block.PlantBlockHelper.isAir;

class HouseBlockScanner extends BlockScanner {
    private int area = 0;
    private int volume = 0;
    private final Map<String/*block.getName()*/, Integer> decorations = new HashMap<>();
    private double temperature = 0;//average temperature
    private final HashSet<AbstractMap.SimpleEntry<Integer, Integer>> occupiedArea = new HashSet<>();

    public int getArea() {
        return this.area;
    }

    public int getVolume() {
        return this.volume;
    }

    public Map<String, Integer> getDecorations() {
        return this.decorations;
    }

    public double getTemperature() {
        return this.temperature;
    }

    public HashSet<AbstractMap.SimpleEntry<Integer, Integer>> getOccupiedArea() {
        return this.occupiedArea;
    }

    HouseBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
    }

    boolean isFloorBlock(BlockPos pos) {
        BlockState blockState = getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
    }

    static boolean isFloorBlock(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
    }

    public static boolean isAirOrLadder(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return isAir(state) || state.isIn(BlockTags.CLIMBABLE) || state.getShape(world, pos).isEmpty();
    }


    boolean isWallBlock(BlockPos pos) {
        BlockState blockState = getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(FHTags.Blocks.WALL_BLOCKS) || blockState.isIn(BlockTags.DOORS) || blockState.isIn(BlockTags.WALLS) || blockState.isIn(Tags.Blocks.GLASS_PANES) || blockState.isIn(Tags.Blocks.FENCE_GATES) || blockState.isIn(Tags.Blocks.FENCES));
    }

    boolean isHouseBlock(BlockPos pos) {
        return isFloorBlock(pos) || isWallBlock(pos);
    }

    BlockState getBlockState(BlockPos pos) {
        return world.getBlockState(pos);
    }

    /**
     * Determine whether a block is a valid floor block.
     * <p>
     * A valid floor block is a block that is a normal cube, a stair, or a slab.
     * <p>
     * A valid floor block must have at least 2 blocks above it.
     * <p>
     * A valid floor block must not have any open air above it.
     *
     * @param pos the position of the block
     * @return whether the block is a valid floor block
     */
    boolean isValidFloor(BlockPos pos) {
        // Determine whether the block satisfies type requirements
        if (!isFloorBlock(pos)) return false;
        AbstractMap.SimpleEntry<Integer, Boolean> information = countBlocksAbove(pos, this::isHouseBlock);
        // Determine whether the block has open air above it
        if (!information.getValue()) {
            this.isValid = false;
            //FHMain.LOGGER.debug("HouseScanner: found block open air!");
            return false;
        } else {
            // Determine whether the block has at least 2 blocks above it
            return information.getKey() >= 2;
        }
    }

    /**
     * Given a block scanned, add the block to the decorations map if it is a decoration block.
     *
     * @param pos the position of the block to check
     */
    void addDecoration(BlockPos pos) {
        BlockState blockState = getBlockState(pos);
        Block block = blockState.getBlock();
        if (blockState.isIn(FHTags.Blocks.DECORATIONS) || Objects.requireNonNull(block.getRegistryName()).getNamespace().equals("cfm")) {
            String name = block.toString();
            // If not in the map, add it with a value of 1
            if (decorations.get(name) == null || decorations.get(name) == 0) {
                decorations.put(name, 1);
            } else {
                // If in the map, increment the value
                decorations.put(name, decorations.get(name) + 1);
            }
        }
    }

    /**
     * Given a floor block, find all possible floor blocks that are adjacent to it.
     *
     * @param startPos the position of the floor block
     * @return a set of possible floor blocks
     */
    public HashSet<BlockPos> nextScanningBlocks(BlockPos startPos) {
        HashSet<BlockPos> possibleFloors = getPossibleFloor(startPos);
        HashSet<BlockPos> possibleFloorsNearLadder = new HashSet<>();
        if (getBlockState(startPos.up()).isIn(BlockTags.CLIMBABLE) || getBlockState(startPos.up(2)).isIn(BlockTags.CLIMBABLE)) {
            for (BlockPos ladder : getBlocksAboveAndBelow(startPos.up(), (pos) -> !(getBlockState(pos).isIn(BlockTags.CLIMBABLE)))) {
                if (!getBlockState(ladder.up()).isNormalCube(world, ladder.up()))
                    possibleFloorsNearLadder.addAll(getPossibleFloor(ladder));
            }
        }
        for (BlockPos blockPos : possibleFloors) {
            if (getBlockState(blockPos).isIn(BlockTags.CLIMBABLE) || getBlockState(blockPos.up()).isIn(BlockTags.CLIMBABLE)) {
                for (BlockPos ladder : getBlocksAboveAndBelow(blockPos, (pos) -> !(getBlockState(pos).isIn(BlockTags.CLIMBABLE)))) {
                    if (!getBlockState(ladder.up()).isNormalCube(world, ladder.up()))
                        possibleFloorsNearLadder.addAll(getPossibleFloor(ladder));
                }
            }
        }
        HashSet<Long> longSet = toLongSet(possibleFloors);
        longSet.addAll(toLongSet(possibleFloorsNearLadder));
        possibleFloors = toPosSet(longSet);

        HashSet<BlockPos> nextScanningBlocks = new HashSet<>();
        for (BlockPos possibleBlock : possibleFloors) {
            if (scannedBlocks.contains(possibleBlock.toLong())) {
                continue;
            }
            if (!isValidFloor(possibleBlock)) {
                scannedBlocks.add(possibleBlock.toLong());
                continue;
            }
            nextScanningBlocks.add(possibleBlock);
        }
        return nextScanningBlocks;
    }

    /**
     * Run the house scanner.
     *
     * @return whether the house is valid
     */
    public boolean check() {
        //第一次扫描，确定地板的位置，并判断是否有露天的地板
        this.scan(512, (pos) -> {
            this.area++;
            this.occupiedArea.add(new AbstractMap.SimpleEntry<>(pos.getX(), pos.getZ()));
            //FHMain.LOGGER.debug("HouseScanner: scanning floor pos " + pos);
        }, this::nextScanningBlocks, (pos) -> !this.isValid);
        //FHMain.LOGGER.debug("HouseScanner: first scan area: " + area);
        if (this.area < 3) this.isValid = false;
        if (!this.isValid) return false;
        //FHMain.LOGGER.debug("HouseScanner: first scan completed");

        //第二次扫描，判断房间是否密闭
        BlockScanner airScanner = new BlockScanner(world, startPos.up());
        airScanner.scan(4096, (pos) -> {//对每一个空气方块执行的操作：统计温度、统计体积、统计温度
                    this.temperature += ChunkHeatData.getTemperature(world, pos);
                    this.volume++;
                }, (pos) -> {//接下来是找到下一批需要扫描的方块的内容
                    HashSet<BlockPos> blocks = new HashSet<>();//这个HashSet暂存下一批的ScanningBlock
                    blocks.addAll(BlockScanner.getBlocksAdjacent/*先找到与本次扫描方块相邻的空气方块*/(pos, (pos1) -> {
                        if (airScanner.getScannedBlocks().contains(pos1.toLong())) return false;
                        if (!isAirOrLadder(world, pos1)) {
                            addDecoration(pos1);
                            return false;
                        }
                        if (!occupiedArea.contains(new AbstractMap.SimpleEntry<>(pos1.getX(), pos1.getZ()))) {/*判断这个空气方块是否在合法地板上方。如果不在则进行更多的操作*/
                            AtomicBoolean isOpenAir = new AtomicBoolean(true);//这里要判断这个空气方块上方是否存在遮挡，但是BlockScanner类里面的getBlocksAbove不返回是否碰到stopAt，因此在这里实现。
                            blocks.addAll(getBlocksAbove(pos1, (pos2) -> {//这个lambda是getBlocksAbove的停止条件
                                if (airScanner.getScannedBlocks().contains(pos2.toLong())) {
                                    isOpenAir.set(false);
                                    return true;//这个方块已经扫描过，那么它上方的方块应该也已经扫描过，那么就不在继续扫描
                                }
                                if (isAirOrLadder(world, pos2)) {
                                    return false;//如果是空气则继续
                                } else {
                                    isOpenAir.set(false);
                                    return true;//碰到非空气的方块的话，就把isOpenAir设置成false。由于isOpenAir初始值为true，如果这里没触发条件把它改成false的话，它就会是true
                                }
                            }));
                            if (isOpenAir.get()) {
                                this.isValid = false;//如果这个空气是露天的话，认为这个房子不是合法的
                                airScanner.isValid = false;
                                return false;
                            }
                        }
                        return isAirOrLadder(world, pos1);
                    }));
                    return deDuplication(blocks);
                },//nextScanningBlocks end
                (useless) -> !this.isValid && !airScanner.isValid);
        temperature /= volume;
        if (this.volume < 6) this.isValid = false;

        return this.isValid;
    }
}
