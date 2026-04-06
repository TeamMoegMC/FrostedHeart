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

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

/**
 * 扫描有效的地板方块
 * 使用模板方法模式，子类可以通过覆写processBlock来定制地板方块的处理逻辑。
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>扫描相邻的地板方块</li>
 *   <li>支持通过梯子连接不同高度的地板（canUseLadder为true时）</li>
 *   <li>验证地板有效性（上方至少有2格空间）</li>
 * </ul>
 */
public class FloorBlockScanner extends AbstractBlockScanner {
    public final boolean canUseLadder;

    public FloorBlockScanner(Level world, BlockPos startPos, int maxScanBlocks) {
        super(world, startPos, maxScanBlocks);
        this.canUseLadder = true;
    }

    public FloorBlockScanner(Level world, BlockPos startPos, boolean canUseLadder, int maxScanBlocks) {
        super(world, startPos, maxScanBlocks);
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

    protected boolean isBuildingBlock(BlockPos pos) {
        return isFloorBlock(pos) || isWallBlock(pos);
    }

    /**
     * 判断方块是否可作为房屋的外壁使用
     */
    public static boolean isBuildingBlock(Level world, BlockPos pos){
        return isFloorBlock(world, pos) || isWallBlock(world, pos);
    }

    public static boolean isValidFloorOrLadder(Level world, BlockPos pos) {
        // Determine whether the block satisfies type requirements
        if (!FloorBlockScanner.isFloorBlock(world, pos) && !world.getBlockState(pos).is(BlockTags.CLIMBABLE)) return false;
        HeightCheckingInfo information = countBlocksAbove(world,pos, (pos1)->FloorBlockScanner.isBuildingBlock(world, pos1));
        // Determine whether the block has open air above it
        if (!information.result()) {
            return false;
        } else {
            // Determine whether the block has at least MIN_ABOVE_HEIGHT blocks above it
            return information.height() >= MIN_ABOVE_HEIGHT;
        }
    }


    /**
     * Determine whether a block is a valid floor block.
     * A valid floor block is a block that is a normal cube, a stair, or a slab.
     * A valid floor block must have at least 2 air blocks above it.
     * A valid floor block must not have any open air above it.
     * 【Override it if you need】
     * @param pos the position of the block
     * @return whether the block is a valid floor block
     */
    public boolean isValidFloor(BlockPos pos) {
        // Determine whether the block satisfies type requirements
        if (!isFloorBlock(pos)) return false;
        HeightCheckingInfo information = countBlocksAbove(world,pos, this::isBuildingBlock);
        // Determine whether the block has open air above it
        if (!information.result()) {
            this.isValid = false;
            return false;
        } else {
            // Determine whether the block has at least MIN_ABOVE_HEIGHT blocks above it
            return information.height() >= MIN_ABOVE_HEIGHT;
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
        // 使用 LongSet 内部处理，最后再转换为 HashSet<BlockPos>
        LongSet possibleFloorsLong = new LongOpenHashSet();
        LongSet visitedForAdd = new LongOpenHashSet();

        // 添加基础可能地板
        for (BlockPos pos : getPossibleFloor(startPos)) {
            possibleFloorsLong.add(pos.asLong());
        }

        // 预获取起始位置附近的 BlockState，减少重复查询
        BlockState stateAbove = getBlockState(startPos.above());
        BlockState stateAbove2 = getBlockState(startPos.above(2));
        boolean hasClimbableAbove = stateAbove.is(BlockTags.CLIMBABLE) || stateAbove2.is(BlockTags.CLIMBABLE);

        if(canUseLadder) {
            if (hasClimbableAbove) {
                for (BlockPos ladder : getBlocksAboveAndBelow(startPos.above(), (pos) -> !(getBlockState(pos).is(BlockTags.CLIMBABLE)))) {
                    if (isValidLadder(ladder)) {
                        for (BlockPos pos : getPossibleFloor(ladder)) {
                            possibleFloorsLong.add(pos.asLong());
                        }
                    }
                }
            }
            // 使用 LongSet 迭代避免重复处理
            for (long lpos : possibleFloorsLong) {
                BlockPos blockPos = BlockPos.of(lpos);
                BlockState state = getBlockState(blockPos);
                BlockState stateAboveTemp = getBlockState(blockPos.above());
                if (state.is(BlockTags.CLIMBABLE) || stateAboveTemp.is(BlockTags.CLIMBABLE)) {
                    for (BlockPos ladder : getBlocksAboveAndBelow(blockPos, (pos) -> !(getBlockState(pos).is(BlockTags.CLIMBABLE)))) {
                        if (isValidLadder(ladder)) {
                            for (BlockPos pos : getPossibleFloorNearLadder(ladder)) {
                                possibleFloorsLong.add(pos.asLong());
                            }
                        }
                    }
                }
            }
        }
        // 内部处理去重和验证
        HashSet<BlockPos> nextScanningBlocks = new HashSet<>();
        for (long key : possibleFloorsLong) {
            if (scannedBlocks.contains(key) || scanningBlocks.contains(key)) {
                continue;
            }
            BlockPos possibleBlock = BlockPos.of(key);
            if (!isValidFloor(possibleBlock)) {
                scannedBlocks.add(key);
                continue;
            }
            if (!visitedForAdd.contains(key)) {
                visitedForAdd.add(key);
                nextScanningBlocks.add(possibleBlock);
            }
        }
        return nextScanningBlocks;
    }
}