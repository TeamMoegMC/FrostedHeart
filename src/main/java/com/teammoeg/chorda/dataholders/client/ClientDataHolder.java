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

public class ClientDataHolder extends DataHolderMap<ClientDataHolder> {
	public ClientDataHolder() {
		super("ClientData");
	}
	boolean isDirty;
	Object lock=new Object();
	public void markDirty() {
		synchronized(lock) {
			isDirty=true;
		}
	}
	boolean getAndClearDirty(){
		boolean ret=isDirty;
		isDirty=false;
		return ret;
	}
}
