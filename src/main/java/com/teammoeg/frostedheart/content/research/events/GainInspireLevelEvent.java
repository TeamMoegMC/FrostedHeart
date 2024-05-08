package com.teammoeg.frostedheart.content.research.events;

import net.minecraftforge.eventbus.api.Event;

public class GainInspireLevelEvent extends Event{
	int level;
	boolean isPersistent;
	boolean gainedPoint;
	public GainInspireLevelEvent(int level, boolean isPersistent, boolean gainedPoint) {
		super();
		this.level = level;
		this.isPersistent = isPersistent;
		this.gainedPoint = gainedPoint;
	}
	
	
}
