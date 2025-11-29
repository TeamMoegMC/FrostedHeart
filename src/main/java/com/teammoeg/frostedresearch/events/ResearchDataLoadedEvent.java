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

package com.teammoeg.frostedresearch.events;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

/**
 * This event is fired when data of a specific team research is loaded
 * data providers should capture this event in order to properly initialize team data
 */
public class ResearchDataLoadedEvent extends Event{
	@Getter
	private final TeamDataHolder teamData;
	@Getter
	private final TeamResearchData researchData;
	public ResearchDataLoadedEvent(TeamDataHolder teamData, TeamResearchData researchData) {
		super();
		this.teamData = teamData;
		this.researchData = researchData;
	}

	
	
}
