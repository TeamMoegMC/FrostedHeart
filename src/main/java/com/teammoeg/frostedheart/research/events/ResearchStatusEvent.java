package com.teammoeg.frostedheart.research.events;

import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraftforge.eventbus.api.Event;

public class ResearchStatusEvent extends Event {
	Research research;
	Team team;
	boolean completion;
	public Research getResearch() {
		return research;
	}
	public boolean isCompletion() {
		return completion;
	}
	public ResearchStatusEvent(Research research, Team team, boolean completion) {
		this.research = research;
		this.team = team;
		this.completion = completion;
	}
	public Team getTeam() {
		return team;
	}
	
}
