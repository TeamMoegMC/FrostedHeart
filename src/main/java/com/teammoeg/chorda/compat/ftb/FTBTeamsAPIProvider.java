package com.teammoeg.chorda.compat.ftb;

import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.TeamsAPIProvider;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;

public class FTBTeamsAPIProvider implements TeamsAPIProvider {

	public FTBTeamsAPIProvider() {
	}

	@Override
	public AbstractTeam getTeamByPlayer(ServerPlayer player) {
		return FTBTeamsAPI.api().getManager().getTeamForPlayer(player).map(FTBTeam::new).orElse(null);
	}

	@Override
	public AbstractTeam getTeamByUuid(UUID uuid) {
		return FTBTeamsAPI.api().getManager().getTeamByID(uuid).map(FTBTeam::new).orElse(null);
	}

	@Override
	public String getProviderName() {
		return "ftbteams";
	}

}
