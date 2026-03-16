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

package com.teammoeg.chorda.block.entity;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块状态访问接口，提供对方块状态的读取和修改能力。
 * 方块实体实现此接口后可以方便地获取和设置其对应的方块状态。
 * <p>
 * Interface for block state access, providing the ability to read and modify block states.
 * Block entities implementing this interface can conveniently get and set their corresponding block state.
 */
public interface BlockStateAccess {

	/**
	 * 获取当前方块状态。
	 * <p>
	 * Gets the current block state.
	 *
	 * @return 当前方块状态 / the current block state
	 */
	public BlockState getBlock();

	/**
	 * 设置方块状态。
	 * <p>
	 * Sets the block state.
	 *
	 * @param state 新的方块状态 / the new block state
	 */
	public void setBlock(BlockState state);
}
