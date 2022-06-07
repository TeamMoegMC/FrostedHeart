package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.network.PacketBuffer;

/**
 * Very Custom Clue trigger by code or manually.
 * 
 */
public class CustomClue extends Clue {
	public CustomClue(String name, float contribution) {
		super(name, contribution);
	}

	public CustomClue(JsonObject jo) {
		super(jo);
	}

	public CustomClue(PacketBuffer pb) {
		super(pb);
	}

	public CustomClue(String name, String desc, String hint, float contribution) {
		super(name, desc, hint, contribution);
	}

	@Override
	public String getId() {
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

	@Override
	public int getIntType() {
		return 0;
	}

}
