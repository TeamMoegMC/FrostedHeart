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

/**
 * 可同步的方块实体接口，提供客户端-服务器数据同步能力。
 * <p>
 * Interface for syncable block entities, providing client-server data synchronization capability.
 */
public interface SyncableBlockEntity {

	/**
	 * 将方块实体数据同步到客户端。
	 * <p>
	 * Synchronizes the block entity data to clients.
	 */
	void syncData();
}
