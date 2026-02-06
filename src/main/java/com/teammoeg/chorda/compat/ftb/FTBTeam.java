/*
 * Copyright (c) 2026 TeamMoeg
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
