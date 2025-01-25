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

package com.teammoeg.chorda.events;

import com.teammoeg.chorda.team.TeamDataHolder;

import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

/**
 * This event is fired when data of a specific team is loaded
 * data providers should capture this event in order to properly initialize team data
 */
public class TeamLoadedEvent extends Event{
	@Getter
	private final TeamDataHolder teamData;
	public TeamLoadedEvent(TeamDataHolder teamData) {
		super();
		this.teamData = teamData;
	}

	
	
}
