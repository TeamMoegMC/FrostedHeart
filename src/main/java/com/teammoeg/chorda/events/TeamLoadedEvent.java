
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
