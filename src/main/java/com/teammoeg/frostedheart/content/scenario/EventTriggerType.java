package com.teammoeg.frostedheart.content.scenario;

import java.util.Objects;

public class EventTriggerType {
	public static final EventTriggerType PLAYER_INTERACT=new EventTriggerType("interact");
	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		EventTriggerType other = (EventTriggerType) obj;
		return Objects.equals(name, other.name);
	}

	private String name;

	public EventTriggerType(String name) {
		super();
		this.name = name;
	}
}
