package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;

public class CachedWorld implements AbstractWorld {
	Map<BlockPos,BlockType> cache=new HashMap<>();
	AbstractWorld internal;
	public CachedWorld(AbstractWorld internal) {
		super();
		this.internal = internal;
	}

	@Override
	public BlockType getBlockType(BlockPos pos) {
		if(getMaxY(pos)>pos.getY())
			return BlockType.AIR;
		
		return cache.computeIfAbsent(pos.immutable(), internal::getBlockType);
	}

	@Override
	public int getMaxY(BlockPos pos) {
		return internal.getMaxY(pos);
	}

}
