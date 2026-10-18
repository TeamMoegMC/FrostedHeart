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