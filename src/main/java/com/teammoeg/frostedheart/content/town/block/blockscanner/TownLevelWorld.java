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
