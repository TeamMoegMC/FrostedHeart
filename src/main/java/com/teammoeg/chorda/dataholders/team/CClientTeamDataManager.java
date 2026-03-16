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

package com.teammoeg.chorda.dataholders.team;

import java.util.UUID;

/**
 * 客户端同步的团队数据管理器。维护一个客户端侧的 {@link TeamDataHolder} 实例，
 * 用于接收和存储从服务器同步过来的团队数据。
 * <p>
 * Client-side synced team data manager. Maintains a client-side {@link TeamDataHolder} instance
 * for receiving and storing team data synchronized from the server.
 */
public class CClientTeamDataManager {
	
	/** 单例实例。 / The singleton instance. */
	public static CClientTeamDataManager INSTANCE=new CClientTeamDataManager();
	
	private TeamDataHolder holder=new TeamDataHolder(UUID.randomUUID(),new ClientTeam());
	private CClientTeamDataManager() {
	}
	
	/**
	 * 重置客户端的所有团队数据，创建一个新的空数据持有者。
	 * <p>
	 * Resets all team data on the client, creating a new empty data holder.
	 */
	public void reset() {
		holder=new TeamDataHolder(UUID.randomUUID(),new ClientTeam());
	}
	
	/**
	 * 获取客户端团队数据持有者实例。
	 * <p>
	 * Gets the client team data holder instance.
	 *
	 * @return 客户端团队数据持有者 / the client team data holder
	 */
	public TeamDataHolder getInstance() {
		return holder;
	}
}
