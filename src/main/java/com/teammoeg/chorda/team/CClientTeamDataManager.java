/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.team;

import java.util.UUID;

import com.teammoeg.chorda.util.struct.OptionalLazy;

/**
 * Synced client data manager.
 */
public class CClientTeamDataManager {
	
	/** The instance. */
	public static CClientTeamDataManager INSTANCE=new CClientTeamDataManager();
	
	private TeamDataHolder holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	private CClientTeamDataManager() {
	}
	
	/**
	 * Reset all data for client.
	 */
	public void reset() {
		holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	}
	
	/**
	 * Gets the single instance of CClientTeamDataManager.
	 *
	 * @return single instance of CClientTeamDataManager
	 */
	public TeamDataHolder getInstance() {
		return holder;
	}
}
