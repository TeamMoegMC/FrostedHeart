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

import net.minecraft.core.BlockPos;

/**
 * 连接方块实体接口，用于具有主从关系的多方块结构中的方块实体。
 * 实现此接口的方块实体可以获取其主方块的位置。
 * <p>
 * Interface for connected block entities that are part of a master-slave multiblock structure.
 * Block entities implementing this interface can retrieve the position of their master block.
 */
public interface ConnectedBlockEntity {

	/**
	 * 获取主方块实体的位置。
	 * <p>
	 * Gets the position of the master block entity.
	 *
	 * @return 主方块的位置 / the position of the master block
	 */
	public BlockPos getMasterPos();
}
