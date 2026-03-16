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

package com.teammoeg.chorda.dataholders.client;

import com.teammoeg.chorda.dataholders.DataHolderMap;

/**
 * 客户端本地数据的持有者，继承自 {@link DataHolderMap}。
 * 支持脏标记机制，当数据被修改时标记为脏，以便 {@link CClientDataStorage} 在适当时机保存。
 * <p>
 * Client-local data holder, extending {@link DataHolderMap}.
 * Supports a dirty flag mechanism; when data is modified it is marked dirty so {@link CClientDataStorage} can save at the appropriate time.
 */
public class ClientDataHolder extends DataHolderMap<ClientDataHolder> {
	/**
	 * 构造一个新的客户端数据持有者。
	 * <p>
	 * Constructs a new client data holder.
	 */
	public ClientDataHolder() {
		super("ClientData");
	}
	boolean isDirty;
	Object lock=new Object();
	/**
	 * 将数据标记为脏（已修改），以便后续保存。
	 * <p>
	 * Marks the data as dirty (modified) for later saving.
	 */
	public void markDirty() {
		synchronized(lock) {
			isDirty=true;
		}
	}
	/**
	 * 获取并清除脏标记。如果数据已被修改，返回 true 并重置标记。
	 * <p>
	 * Gets and clears the dirty flag. Returns true if data was modified and resets the flag.
	 *
	 * @return 数据是否曾被修改 / whether the data had been modified
	 */
	boolean getAndClearDirty(){
		boolean ret=isDirty;
		isDirty=false;
		return ret;
	}
}
