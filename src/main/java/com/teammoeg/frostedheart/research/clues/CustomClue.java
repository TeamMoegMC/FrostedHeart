package com.teammoeg.frostedheart.research.clues;

import dev.ftb.mods.ftbteams.data.Team;

/**
 * Very Custom Clue trigger by code or manually.
 * 
 * */
public class CustomClue extends Clue{
	public CustomClue(String name, float contribution) {
		super(name, contribution);
	}

	@Override
	public String getType() {
		return "custom";
	}

	@Override
	public void init() {
	}

	@Override
	public void start(Team team) {
	}

	@Override
	public void end(Team team) {
	}


}
