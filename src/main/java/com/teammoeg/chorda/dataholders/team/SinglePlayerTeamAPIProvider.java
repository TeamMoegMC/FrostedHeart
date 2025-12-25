package com.teammoeg.chorda.dataholders.team;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public class SinglePlayerTeamAPIProvider implements TeamsAPIProvider {

	public SinglePlayerTeamAPIProvider() {
	}

	@Override
	public AbstractTeam getTeamByPlayer(ServerPlayer p) {
		return new SinglePlayerTeam(p.getUUID());
	}

	@Override
	public AbstractTeam getTeamByUuid(UUID uuid) {
		return new SinglePlayerTeam(uuid);
	}

	@Override
	public String getProviderName() {
		return "chorda";
	}

}
