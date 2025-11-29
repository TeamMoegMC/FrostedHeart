package com.teammoeg.chorda.events;

import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class PlayerTeamChangedEvent extends Event{
	public final AbstractTeam originalTeam;
	public final AbstractTeam team;
	public final ServerPlayer player;
	public PlayerTeamChangedEvent( AbstractTeam originalTeam, AbstractTeam team, ServerPlayer player) {
		super();
		this.originalTeam = originalTeam;
		this.team = team;
		this.player = player;
	}




}
