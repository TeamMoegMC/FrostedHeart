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

package com.teammoeg.chorda.multiblock.components;

import java.util.UUID;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;

/**
 * 多方块所有者状态接口，定义了所有者（通常为玩家或队伍）的管理能力。
 * 实现此接口的多方块状态可以跟踪和管理多方块的所有者，并在所有者变更时执行回调。
 * <p>
 * Multiblock owner state interface that defines ownership management capabilities
 * (typically for players or teams). Multiblock states implementing this interface can track
 * and manage the multiblock's owner and execute callbacks when ownership changes.
 *
 * @param <T> 多方块状态类型 / The multiblock state type
 * @see OwnerState
 */
public interface IOwnerState<T> {

	/**
	 * 获取当前所有者的 UUID。
	 * <p>
	 * Gets the UUID of the current owner.
	 *
	 * @return 所有者的 UUID，如果没有所有者则可能为 null / The owner's UUID, may be null if there is no owner
	 */
	UUID getOwner();

	/**
	 * 设置多方块的所有者。
	 * <p>
	 * Sets the owner of the multiblock.
	 *
	 * @param owner 新所有者的 UUID / The new owner's UUID
	 */
	void setOwner(UUID owner);

	/**
	 * 当所有者发生变更时调用的回调方法。可用于更新权限、刷新缓存或通知相关系统。
	 * <p>
	 * Callback method invoked when the owner changes. Can be used to update permissions,
	 * refresh caches, or notify related systems.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 */
	void onOwnerChange(IMultiblockContext<? extends T> ctx);
}