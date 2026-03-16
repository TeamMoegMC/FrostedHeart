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

package com.teammoeg.chorda.events;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;

import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

/**
 * 当队伍数据从存储中加载时触发的事件。数据提供者应监听此事件以正确初始化队伍数据。
 * <p>
 * Event fired when team data is loaded from storage.
 * Data providers should listen to this event to properly initialize team data.
 */
public class TeamLoadedEvent extends Event{
	/** 已加载的队伍数据持有者 / The loaded team data holder */
	@Getter
	private final TeamDataHolder teamData;
	/**
	 * 创建队伍加载事件。
	 * <p>
	 * Creates a team loaded event.
	 *
	 * @param teamData 已加载的队伍数据持有者 / The loaded team data holder
	 */
	public TeamLoadedEvent(TeamDataHolder teamData) {
		super();
		this.teamData = teamData;
	}

	
	
}
