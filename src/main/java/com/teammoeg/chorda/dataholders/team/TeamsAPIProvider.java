package com.teammoeg.chorda.dataholders.team;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public interface TeamsAPIProvider {
	AbstractTeam getTeamByPlayer(ServerPlayer p);
	AbstractTeam getTeamByUuid(UUID uuid);
	String getProviderName();
}
