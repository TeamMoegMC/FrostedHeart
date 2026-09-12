package com.teammoeg.frostedheart.content.town.block.blockscanner;

import net.minecraft.core.BlockPos;

public interface AbstractWorld {
	public static enum BlockType {
	    LADDER,
	    AIR,
	    WALL,
	    DECO
	}
    /**
     * 获取指定坐标的方块类型。
     * 实现留空，由使用者补充。
     */
    BlockType getBlockType(BlockPos pos);
    int getMaxY(BlockPos pos);
}