package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.network.PacketBuffer;

/**
 * Clue with listener trigger
 */
public abstract class ListenerClue extends Clue {
	public boolean alwaysOn;

	public ListenerClue(String name, float contribution) {
		super(name, contribution);
	}

	public ListenerClue(String name, String desc, String hint, float contribution) {
		super(name, desc, hint, contribution);
	}

	public ListenerClue(JsonObject jo) {
		super(jo);
		alwaysOn = jo.get("always").getAsBoolean();
	}

	public ListenerClue(PacketBuffer pb) {
		super(pb);
		alwaysOn = pb.readBoolean();
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo = super.serialize();
		jo.addProperty("always", alwaysOn);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeBoolean(alwaysOn);
	}

	@Override
	public void init() {
		if (alwaysOn)
			initListener(null);
	}

	@Override
	public void start(Team team) {
		if (!alwaysOn)
			initListener(team);

	}

	@Override
	public void end(Team team) {
		removeListener(team);
	}

	public abstract void initListener(Team t);

	public abstract void removeListener(Team t);

}
