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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import lombok.Setter;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.player.CachedBlockTempInfo;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
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
public class RoomSpaceScanner {
    public final boolean canUseLadder;

    @Getter
    protected LongSet scannedBlocks;
    @Getter
    @Setter
    protected LongSet scanningBlocks;
    protected HashSet<BlockPos> scanningBlocksNew = new HashSet<>();
    public final int maxScanBlocks;
    protected final BlockPos startPos;
    public final Level world;
    public static final Direction[] PLANE_DIRECTIONS= {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
    public boolean isValid = true;//it can be changed in methods, scan should stop when this is false

    public RoomSpaceScanner(Level world, BlockPos startPos, int maxScanBlocks) {
    	this(world, startPos, true, maxScanBlocks);
    }

    public RoomSpaceScanner(Level world, BlockPos startPos, boolean canUseLadder, int maxScanBlocks) {
        this.startPos = startPos;
        this.maxScanBlocks = maxScanBlocks;
        this.scanningBlocks = new LongOpenHashSet();
        this.scanningBlocks.add(startPos.asLong());
        //FHMain.LOGGER.debug("HouseScanner: scanningBlocks: " + scanningBlocks);
        this.world = world;
        this.scannedBlocks = new LongOpenHashSet();
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

    private static final Map<BlockState, AABB> BOUNDS_CACHE = new HashMap<>(256);
    public static AABB queryBounds(Level world, BlockPos pos,BlockState blockState) {
        
        AABB bounds=BOUNDS_CACHE.get(blockState);
        if(bounds!=null)
        	return bounds;
        bounds=blockState.getCollisionShape(world, pos).bounds();
        BOUNDS_CACHE.put(blockState, bounds);
        return bounds;
    }
    public static enum PassableType implements Iterable<Direction>{
    	NO(List.of()),
    	X(List.of(Direction.EAST,Direction.WEST)),
    	Z(List.of(Direction.NORTH,Direction.SOUTH)),
    	ALL(List.of(Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH));
		List<Direction> dirs;
    	private PassableType(List<Direction> dirs) {
			this.dirs = dirs;
		}
		@Override
		public Iterator<Direction> iterator() {
			return dirs.iterator();
		}

    }
    public static PassableType isPassageBlock(Level world, BlockPos pos) {
    	BlockState blockState = world.getBlockState(pos);
        AABB bounds=queryBounds(world,pos,blockState);
        boolean Zblock=bounds.getXsize()>=0.5;
        boolean Xblock=bounds.getZsize()>=0.5;
        if(Xblock&&Zblock)
        	return PassableType.NO;
        if(blockState.is(FHTags.Blocks.TOWN_WALLS.tag) || blockState.is(BlockTags.DOORS) || blockState.is(BlockTags.WALLS) || blockState.is(Tags.Blocks.GLASS_PANES) || blockState.is(Tags.Blocks.FENCE_GATES) || blockState.is(Tags.Blocks.FENCES))
        	return PassableType.NO;
        if(Xblock)
        	return PassableType.Z;
        if(Zblock)
        	return PassableType.X;
        return PassableType.ALL;
    }
    protected PassableType isPassageBlock(BlockPos pos) {
        return isPassageBlock(this.world, pos);
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
        if (!RoomSpaceScanner.isFloorBlock(world, pos) && !world.getBlockState(pos).is(BlockTags.CLIMBABLE)) return false;
        HeightCheckingInfo information = countBlocksAbove(world,pos, (pos1)->RoomSpaceScanner.isBuildingBlock(world, pos1));
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
    public static enum FloorType{
    	NO,
    	BELOW,
    	ABOVE;
    }
    public FloorType canBeFloor(BlockPos pos) {
    	BlockState state=world.getBlockState(pos);
    	if(state.is(FHTags.Blocks.TOWN_DECORATIONS.get()))
    		return FloorType.NO;
    	if(state.isFaceSturdy(world, pos, Direction.DOWN))
    		return FloorType.BELOW;
    	if(state.isFaceSturdy(world, pos, Direction.UP))
    		return FloorType.ABOVE;
    	return FloorType.NO;
    }
    /**
     * Given a floor block, find all possible floor blocks that are adjacent to it.
     *
     * @param startPos the position of the floor block
     * @return a set of possible floor blocks
     */
    protected void scanPos(BlockPos startPos) {
        // 使用 LongSet 内部处理，最后再转换为 HashSet<BlockPos>
    	HashSet<BlockPos> possibleFloorsLong = new HashSet<>();
        LongSet visitedForAdd = new LongOpenHashSet();
        HashSet<BlockPos> laddersToExpand = new HashSet<BlockPos>();

        // 添加基础可能地板
        for (Direction dir:Direction.Plane.HORIZONTAL) {
        	BlockPos floor=startPos.relative(dir);
        	int y=floor.getY();
        	BlockPos.MutableBlockPos floorAdjacent=new MutableBlockPos();
        	floorAdjacent.set(floor);
        	floorAdjacent.setY(y+1);
        	switch(canBeFloor(floorAdjacent)) {
        	case BELOW:possibleFloorsLong.add(floorAdjacent.immutable());floorAdjacent.setY(y);
        	case ABOVE:possibleFloorsLong.add(floorAdjacent.immutable());
        	default:
        		continue;
        	}
        	floorAdjacent.setY(y);
        	switch(canBeFloor(floorAdjacent)) {
        	case BELOW:possibleFloorsLong.add(floorAdjacent.immutable());floorAdjacent.setY(y-1);
        	case ABOVE:possibleFloorsLong.add(floorAdjacent.immutable());
        	default:
        		continue;
        	}
        	floorAdjacent.setY(y-1);
        	switch(canBeFloor(floorAdjacent)) {
        	case ABOVE:possibleFloorsLong.add(floorAdjacent.immutable());
        	default:
        		continue;
        	}
        }


        if(canUseLadder) {
            // 使用 LongSet 迭代避免重复处理
            for (BlockPos lpos : possibleFloorsLong) {
                BlockState state = world.getBlockState(lpos);
                BlockPos.MutableBlockPos mpos=new MutableBlockPos();
                mpos.set(lpos);
                int y=mpos.getY();
                mpos.setY(y+1);
                BlockState stateAboveTemp = world.getBlockState(lpos.above());
                if (state.isLadder(world, lpos, null)||stateAboveTemp.isLadder(world, mpos, null)) {
                    for (BlockPos ladder : getBlocksAboveAndBelow(world, blockPos, (pos) -> !(getBlockState(pos).is(BlockTags.CLIMBABLE)))) {
                        if (isValidLadder(ladder)) {
                            laddersToExpand.add(ladder.asLong());
                        }
                    }
                }
            }
            for (long ladderLong : laddersToExpand) {
                for (BlockPos pos : getPossibleFloorNearLadder(BlockPos.of(ladderLong))) {
                    possibleFloorsLong.add(pos.asLong());
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