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

package com.teammoeg.frostedheart.base.team;

import java.util.UUID;

import com.teammoeg.frostedheart.util.OptionalLazy;

/**
 * Synced client data manager.
 */
public class ClientTeamDataManager {
	
	/** The instance. */
	public static ClientTeamDataManager INSTANCE=new ClientTeamDataManager();
	
	private TeamDataHolder holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	private ClientTeamDataManager() {
	}
	
	/**
	 * Reset all data for client.
	 */
	public void reset() {
		holder=new TeamDataHolder(UUID.randomUUID(),OptionalLazy.empty());
	}
	
	/**
	 * Gets the single instance of ClientTeamDataManager.
	 *
	 * @return single instance of ClientTeamDataManager
	 */
	public TeamDataHolder getInstance() {
		return holder;
	}
}
