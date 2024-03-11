package com.teammoeg.frostedheart.util.blockscanner;

import com.teammoeg.frostedheart.FHTags;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * scan valid floor
 */
public class FloorBlockScanner extends BlockScanner{
    public final boolean canUseLadder;

    public FloorBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
        this.canUseLadder = true;
    }

    public FloorBlockScanner(World world, BlockPos startPos, boolean canUseLadder) {
        super(world, startPos);
        this.canUseLadder = canUseLadder;
    }

    protected boolean isFloorBlock(BlockPos pos) {
        BlockState blockState = getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
    }

    public static boolean isFloorBlock(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
    }

    public static boolean isWallBlock(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(FHTags.Blocks.WALL_BLOCKS) || blockState.isIn(BlockTags.DOORS) || blockState.isIn(BlockTags.WALLS) || blockState.isIn(Tags.Blocks.GLASS_PANES) || blockState.isIn(Tags.Blocks.FENCE_GATES) || blockState.isIn(Tags.Blocks.FENCES));
    }
    protected boolean isWallBlock(BlockPos pos) {
        return isWallBlock(this.world, pos);
    }

    protected boolean isHouseBlock(BlockPos pos) {
        return isFloorBlock(pos) || isWallBlock(pos);
    }

    public static boolean isHouseBlock(World world, BlockPos pos){
        return isFloorBlock(world, pos) || isWallBlock(world, pos);
    }

    public static boolean isValidFloorOrLadder(World world, BlockPos pos) {
        // Determine whether the block satisfies type requirements
        if (!FloorBlockScanner.isFloorBlock(world, pos) && !world.getBlockState(pos).isIn(BlockTags.CLIMBABLE)) return false;
        AbstractMap.SimpleEntry<Integer, Boolean> information = countBlocksAbove(pos, (pos1)->FloorBlockScanner.isHouseBlock(world, pos1));
        // Determine whether the block has open air above it
        if (!information.getValue()) {
            return false;
        } else {
            // Determine whether the block has at least 2 blocks above it
            return information.getKey() >= 2;
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

    protected boolean isValidLadder(BlockPos pos){
        return world.getBlockState(pos).isIn(BlockTags.CLIMBABLE) && isAirOrLadder(world, pos.up()) && isAirOrLadder(world, pos.up(2));
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
            if (getBlockState(startPos.up()).isIn(BlockTags.CLIMBABLE) || getBlockState(startPos.up(2)).isIn(BlockTags.CLIMBABLE)) {
                for (BlockPos ladder : getBlocksAboveAndBelow(startPos.up(), (pos) -> !(getBlockState(pos).isIn(BlockTags.CLIMBABLE)))) {
                    if (isValidLadder(ladder))
                        possibleFloorsNearLadder.addAll(getPossibleFloor(ladder));
                }
            }
            for (BlockPos blockPos : possibleFloors) {
                if (getBlockState(blockPos).isIn(BlockTags.CLIMBABLE) || getBlockState(blockPos.up()).isIn(BlockTags.CLIMBABLE)) {
                    for (BlockPos ladder : getBlocksAboveAndBelow(blockPos, (pos) -> !(getBlockState(pos).isIn(BlockTags.CLIMBABLE)))) {
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