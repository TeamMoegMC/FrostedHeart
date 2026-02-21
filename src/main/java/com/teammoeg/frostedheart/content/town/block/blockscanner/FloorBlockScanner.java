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

package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.HashSet;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

/**
 * scan valid floor
 */
public class FloorBlockScanner extends BlockScanner{
    public final boolean canUseLadder;

    public FloorBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
        this.canUseLadder = true;
    }

    public FloorBlockScanner(Level world, BlockPos startPos, boolean canUseLadder) {
        super(world, startPos);
        this.canUseLadder = canUseLadder;
    }

    protected boolean isFloorBlock(BlockPos pos) {
        return isFloorBlock(world, pos);
    }

    public static boolean isFloorBlock(Level world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        //return !blockState.getCollisionShape(world, pos).isEmpty();
        return (blockState.isCollisionShapeFullBlock(world, pos) || blockState.is(BlockTags.STAIRS) || blockState.is(BlockTags.SLABS));
    }

    public static boolean isWallBlock(Level world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return (blockState.isCollisionShapeFullBlock(world, pos) || blockState.is(FHTags.Blocks.TOWN_WALLS.tag) || blockState.is(BlockTags.DOORS) || blockState.is(BlockTags.WALLS) || blockState.is(Tags.Blocks.GLASS_PANES) || blockState.is(Tags.Blocks.FENCE_GATES) || blockState.is(Tags.Blocks.FENCES));
    }
    protected boolean isWallBlock(BlockPos pos) {
        return isWallBlock(this.world, pos);
    }

    protected boolean isHouseBlock(BlockPos pos) {
        return isFloorBlock(pos) || isWallBlock(pos);
    }

    public static boolean isHouseBlock(Level world, BlockPos pos){
        return isFloorBlock(world, pos) || isWallBlock(world, pos);
    }

    public static boolean isValidFloorOrLadder(Level world, BlockPos pos) {
        // Determine whether the block satisfies type requirements
        if (!FloorBlockScanner.isFloorBlock(world, pos) && !world.getBlockState(pos).is(BlockTags.CLIMBABLE)) return false;
        HeightCheckingInfo information = countBlocksAbove(world,pos, (pos1)->FloorBlockScanner.isHouseBlock(world, pos1));
        // Determine whether the block has open air above it
        if (!information.result()) {
            return false;
        } else {
            // Determine whether the block has at least 2 blocks above it
            return information.height() >= 2;
        }
    }


    /**
     * Determine whether a block is a valid floor block.
     * A valid floor block is a block that is a normal cube, a stair, or a slab.
     * A valid floor block must have at least 2 blocks above it.
     * A valid floor block must not have any open air above it.
     * 【Override it if you need】
     * @param pos the position of the block
     * @return whether the block is a valid floor block
     */
    public boolean isValidFloor(BlockPos pos) {
        // Determine whether the block satisfies type requirements
        if (!isFloorBlock(pos)) return false;
        HeightCheckingInfo information = countBlocksAbove(world,pos, this::isHouseBlock);
        // Determine whether the block has open air above it
        if (!information.result()) {
            this.isValid = false;
            //FHMain.LOGGER.debug("HouseScanner: found block open air!");
            return false;
        } else {
            // Determine whether the block has at least 2 blocks above it
            return information.height() >= 2;
        }
    }

    protected boolean isValidLadder(BlockPos pos){
        return world.getBlockState(pos).is(BlockTags.CLIMBABLE) && isAirOrLadder(world, pos.above()) && isAirOrLadder(world, pos.above(2));
    }

    /**
     * Given a floor block, find all possible floor blocks that are adjacent to it.
     *
     * @param startPos the position of the floor block
     * @return a set of possible floor blocks
     */
    protected HashSet<BlockPos> nextScanningBlocks(BlockPos startPos) {
        HashSet<BlockPos> possibleFloors = new HashSet<>(getPossibleFloor(startPos));
        if(canUseLadder) {
            HashSet<BlockPos> possibleFloorsNearLadder = new HashSet<>();
            if (getBlockState(startPos.above()).is(BlockTags.CLIMBABLE) || getBlockState(startPos.above(2)).is(BlockTags.CLIMBABLE)) {
                for (BlockPos ladder : getBlocksAboveAndBelow(startPos.above(), (pos) -> !(getBlockState(pos).is(BlockTags.CLIMBABLE)))) {
                    if (isValidLadder(ladder))
                        possibleFloorsNearLadder.addAll(getPossibleFloor(ladder));
                }
            }
            for (BlockPos blockPos : possibleFloors) {
                if (getBlockState(blockPos).is(BlockTags.CLIMBABLE) || getBlockState(blockPos.above()).is(BlockTags.CLIMBABLE)) {
                    for (BlockPos ladder : getBlocksAboveAndBelow(blockPos, (pos) -> !(getBlockState(pos).is(BlockTags.CLIMBABLE)))) {
                        if (isValidLadder(ladder))
                            possibleFloorsNearLadder.addAll(getPossibleFloorNearLadder(ladder));
                    }
                }
            }
            possibleFloors.addAll(possibleFloorsNearLadder);
        }
        HashSet<BlockPos> nextScanningBlocks = new HashSet<>();
        for (BlockPos possibleBlock : possibleFloors) {
            if (scannedBlocks.contains(possibleBlock) || scanningBlocks.contains(possibleBlock)) {
                continue;
            }
            if (!isValidFloor(possibleBlock)) {
                scannedBlocks.add(possibleBlock);
                continue;
            }
            nextScanningBlocks.add(possibleBlock);
        }
        return nextScanningBlocks;
    }

    //暂时不需要覆写scan，BlockScanner的scan够用了
}