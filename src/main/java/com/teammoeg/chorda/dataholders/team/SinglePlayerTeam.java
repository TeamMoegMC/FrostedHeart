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

package com.teammoeg.chorda.dataholders.team;

import java.util.Collection;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.teammoeg.chorda.util.CDistHelper;

import net.minecraft.server.level.ServerPlayer;

public record SinglePlayerTeam(UUID player) implements AbstractTeam {

	@Override
	public Collection<ServerPlayer> getOnlineMembers() {
		ServerPlayer s =getPlayer();
		if(s!=null)
		ImmutableList.of(s);
		return ImmutableList.of();
	}
	public ServerPlayer getPlayer() {
		return CDistHelper.getServer().getPlayerList().getPlayer(player);
	}
	@Override
	public UUID getId() {
		return player;
	}

	@Override
	public String getName() {
		ServerPlayer s =getPlayer();
		if(s!=null)
			return s.getName().getString();
		return CDistHelper.getServer().getProfileCache().get(player).get().getName();
	}

	@Override
	public UUID getOwner() {
		return player;
	}

}
