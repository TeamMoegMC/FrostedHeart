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


import java.util.AbstractMap;
import java.util.HashSet;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 扫描空气和密闭空间
 * 可用于判断一个空间是否密闭。
 * 如果需要用某些特定的方块包围密闭空间，可以在子类中覆写isValidAir方法
 *
 * <p>双钩子方法设计：</p>
 * <ul>
 *   <li>{@code processAirBlock(BlockPos)} - 子类可覆写，处理空气方块</li>
 *   <li>{@code processNonAirBlock(BlockPos)} - 子类可覆写，处理非空气方块（围住密闭空间的方块）</li>
 * </ul>
 */
public class ConfinedSpaceScanner extends AbstractBlockScanner {

    public ConfinedSpaceScanner(Level world, BlockPos startPos, int maxScanBlocks){
        super(world, startPos, maxScanBlocks);
    }

    /**
     * 在子类中覆写此方法以修改空气的判定条件。如果你想扫描什么别的方块也可以用这个类并覆写此方法
     */
    protected boolean isValidAir(BlockPos pos){
        return isAirOrLadder(world, pos);
    }

    @Override
    protected void processBlock(BlockPos pos) {
        // 根据方块类型分别处理
        if (isValidAir(pos)) {
            processAirBlock(pos);
        } else {
            processNonAirBlock(pos);
        }
    }

    /**
     * 处理空气方块。子类可以覆写此方法来定义对空气方块的处理逻辑。
     * 默认实现为空。
     * @param pos 空气方块的位置
     */
    protected void processAirBlock(BlockPos pos) {
        // 默认实现为空，子类可以覆写
    }

    /**
     * 处理非空气方块（围住密闭空间的方块）。子类可以覆写此方法来定义对非空气方块的处理逻辑。
     * 默认实现为空。
     * @param pos 非空气方块的位置
     */
    protected void processNonAirBlock(BlockPos pos) {
        // 默认实现为空，子类可以覆写
    }

    @Override
    protected HashSet<BlockPos> nextScanningBlocks(BlockPos pos) {
        // 内部使用 LongSet 处理，最后转换为 HashSet<BlockPos>
        LongSet nextScanningBlocksLong = new LongOpenHashSet();
        LongSet visitedForAdd = new LongOpenHashSet();

        for(Direction direction : Direction.values()){
            BlockPos pos1 = pos.relative(direction);// pos1: 用于存储与pos相邻的方块
            long key1 = pos1.asLong();
            if (this.getScannedBlocks().contains(key1)) continue;
            if (!isValidAir(pos1)) {
                // 非空气方块：添加到scannedBlocks，但不添加到nextScanningBlocks
                // 会通过processBlock被处理
                scannedBlocks.add(key1);
                continue;
            }
            if (!visitedForAdd.contains(key1)) {
                visitedForAdd.add(key1);
                nextScanningBlocksLong.add(key1);
            }

            // 获取上方空气的 LongSet 结果
            AbstractMap.SimpleEntry<LongSet, Boolean> airsAbove = getAirsAboveLong(pos1);
            if(!airsAbove.getValue()){
                this.isValid = false;
                // 转换为 HashSet<BlockPos> 返回
                HashSet<BlockPos> result = new HashSet<>();
                for (Long aLong : nextScanningBlocksLong) {
                    result.add(BlockPos.of(aLong));
                }
                return result;
            }
            // 合并airsAbove中的元素
            LongSet airsAboveSet = airsAbove.getKey();
            for (long lpos : airsAboveSet) {
                if (!visitedForAdd.contains(lpos)) {
                    visitedForAdd.add(lpos);
                    nextScanningBlocksLong.add(lpos);
                }
            }
        }

        // 转换为 HashSet<BlockPos> 返回
        HashSet<BlockPos> result = new HashSet<>();
        for (Long aLong : nextScanningBlocksLong) {
            result.add(BlockPos.of(aLong));
        }
        return result;
    }

    //基本上和getBlocksAbove是相同的，为了减少lambda的使用单列一个方法
    // 返回 LongSet 版本的内部方法
    private AbstractMap.SimpleEntry<LongSet, Boolean> getAirsAboveLong(BlockPos startPos){
        BlockPos scanningBlock;
        scanningBlock = startPos.above();
        LongSet blocks = new LongOpenHashSet();
        while(scanningBlock.getY() <= MAX_HEIGHT){
            long key = scanningBlock.asLong();
            if( scannedBlocks.contains(key) || !isValidAir(scanningBlock) ){
                return new AbstractMap.SimpleEntry<>(blocks, true);
            }
            blocks.add(key);
            scanningBlock = scanningBlock.above();
        }
        return new AbstractMap.SimpleEntry<>(blocks, false);
    }
}
