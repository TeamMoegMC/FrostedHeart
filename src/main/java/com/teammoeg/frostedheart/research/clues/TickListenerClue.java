package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.ResearchListeners;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;

public abstract class TickListenerClue extends ListenerClue {

	public TickListenerClue(String name, String desc, String hint, float contribution) {
		super(name, desc, hint, contribution);
	}

	public TickListenerClue(String name, float contribution) {
		super(name, contribution);
	}

	public TickListenerClue(JsonObject jo) {
		super(jo);
	}

	public TickListenerClue(PacketBuffer pb) {
		super(pb);
	}

	@Override
	public void initListener(Team t) {
		ResearchListeners.getTickClues().add(this, t);
	}

	@Override
	public void removeListener(Team t) {
		ResearchListeners.getTickClues().remove(this, t);
	}

	public final void tick(Team t, ServerPlayerEntity player) {
		if (!this.isCompleted(t))
			if (this.isCompleted(t, player)) {
				this.setCompleted(t, true);
			}
	}

	public abstract boolean isCompleted(Team t, ServerPlayerEntity player);

}
