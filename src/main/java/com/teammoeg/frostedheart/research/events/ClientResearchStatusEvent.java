package com.teammoeg.frostedheart.research.events;

import com.teammoeg.frostedheart.research.Research;

import net.minecraftforge.eventbus.api.Event;

public class ClientResearchStatusEvent extends Event {
	Research research;
	boolean completion;
	public Research getResearch() {
		return research;
	}
	public boolean isCompletion() {
		return completion;
	}
	public ClientResearchStatusEvent(Research research, boolean completion) {
		this.research = research;
		this.completion = completion;
	}
	
}
