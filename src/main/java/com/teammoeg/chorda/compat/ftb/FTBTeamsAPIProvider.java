/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
