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
import com.teammoeg.chorda.client.ClientUtils;

import net.minecraft.server.level.ServerPlayer;

public class ClientTeam implements AbstractTeam {

	public ClientTeam() {

	}

	@Override
	public Collection<ServerPlayer> getOnlineMembers() {
		return ImmutableList.of();
	}
	UUID ZERO=UUID.fromString("11da5049-661a-4435-9742-6ba376291d5b");
	@Override
	public UUID getId() {
		return ZERO;
	}

	@Override
	public String getName() {
		return ClientUtils.getMc().player.getName().getString();
	}

	@Override
	public UUID getOwner() {
		return ClientUtils.getMc().player.getUUID();
	}

}
