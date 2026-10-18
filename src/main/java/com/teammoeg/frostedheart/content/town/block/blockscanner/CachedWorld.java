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
		if(getMaxY(pos)<pos.getY())
			return BlockType.AIR;
		
		BlockType bp=cache.get(pos);
		if(bp==null) {
			bp=internal.getBlockType(pos);
			BlockPos imbp= pos.immutable();
			cache.put(imbp, bp);
		}
		return bp;
	}

	@Override
	public int getMaxY(BlockPos pos) {
		return internal.getMaxY(pos);
	}

}
