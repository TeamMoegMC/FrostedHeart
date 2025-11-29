package com.teammoeg.chorda.compat.ftb;

import java.util.Collection;
import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.AbstractTeam;

import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;

public record FTBTeam(Team team) implements AbstractTeam {
	@Override
	public Collection<ServerPlayer> getOnlineMembers() {
		return team.getOnlineMembers();
	}
	@Override
	public UUID getId() {
		return team.getId();
	}
	@Override
	public String getName() {

		return team.getName().getString();
	}
	@Override
	public UUID getOwner() {
		return team.getOwner();
	}

}
