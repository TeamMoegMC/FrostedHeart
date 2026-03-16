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
 * 当队伍被创建时触发的事件。数据提供者应监听此事件以正确初始化队伍数据。
 * <p>
 * Event fired when a team is created.
 * Data providers should listen to this event to properly initialize team data.
 */
public class TeamCreatedEvent extends Event{
	@Getter
	private final TeamDataHolder teamData;
	/**
	 * 创建队伍创建事件。
	 * <p>
	 * Creates a team created event.
	 *
	 * @param teamData 新创建的队伍数据持有者 / The newly created team data holder
	 */
	public TeamCreatedEvent(TeamDataHolder teamData) {
		super();
		this.teamData = teamData;
	}

	
	
}
