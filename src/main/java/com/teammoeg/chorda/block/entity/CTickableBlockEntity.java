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
 * 可刻更新的方块实体接口，替代旧版的 TickableBlockEntity 以便于集成和迁移。
 * 对应的方块必须实现 {@link com.teammoeg.chorda.block.CEntityBlock} 接口。
 * <p>
 * Interface for tickable block entities, replacing the legacy TickableBlockEntity
 * for easier integration and migration. The corresponding block must implement
 * {@link com.teammoeg.chorda.block.CEntityBlock}.
 */
public interface CTickableBlockEntity {

	/**
	 * 每游戏刻调用一次的更新方法。在服务端和客户端均会被调用。
	 * <p>
	 * Called once per game tick. Invoked on both server and client sides.
	 */
	void tick();
}
