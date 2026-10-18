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

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TownLevelWorld implements AbstractWorld {
	LevelReader level;
	public TownLevelWorld(LevelReader level) {
		super();
		this.level = level;
	}

	@Override
	public BlockType getBlockType(BlockPos pos) {
		BlockState state=level.getBlockState(pos);
		if(state.isAir())
			return BlockType.AIR;
		if(state.isLadder(level, pos, null))
			return BlockType.LADDER;
		if(state.is(FHTags.Blocks.TOWN_FURNITURE.get()))
			return BlockType.DECO;
		VoxelShape shape=state.getCollisionShape(level, pos);
		if(shape.isEmpty())
			return BlockType.AIR;

		AABB aabb=shape.bounds();
		return aabb.getXsize()<0.3&&aabb.getYsize()<0.3&&aabb.getZsize()<0.3?BlockType.AIR:BlockType.WALL;
	}

	@Override
	public int getMaxY(BlockPos pos) {
		return level.getHeight(Types.WORLD_SURFACE, pos.getX(), pos.getZ());
	}

}
