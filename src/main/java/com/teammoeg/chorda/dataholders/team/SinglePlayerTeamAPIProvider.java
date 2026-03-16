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

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/**
 * 单人模式下的团队 API 提供者。为每个玩家创建独立的 {@link SinglePlayerTeam} 实例。
 * 当 FTB Teams 模组不可用时，作为默认的团队 API 实现。
 * <p>
 * Single-player team API provider. Creates independent {@link SinglePlayerTeam} instances for each player.
 * Serves as the default team API implementation when the FTB Teams mod is not available.
 */
public class SinglePlayerTeamAPIProvider implements TeamsAPIProvider {

	/**
	 * 构造一个新的单人团队 API 提供者。
	 * <p>
	 * Constructs a new single-player team API provider.
	 */
	public SinglePlayerTeamAPIProvider() {
	}

	/**
	 * {@inheritDoc}
	 * 根据玩家 UUID 创建一个新的 {@link SinglePlayerTeam}。
	 * <p>
	 * Creates a new {@link SinglePlayerTeam} from the player's UUID.
	 */
	@Override
	public AbstractTeam getTeamByPlayer(ServerPlayer p) {
		return new SinglePlayerTeam(p.getUUID());
	}

	/**
	 * {@inheritDoc}
	 * 根据 UUID 创建一个新的 {@link SinglePlayerTeam}。
	 * <p>
	 * Creates a new {@link SinglePlayerTeam} from the given UUID.
	 */
	@Override
	public AbstractTeam getTeamByUuid(UUID uuid) {
		return new SinglePlayerTeam(uuid);
	}

	/** {@inheritDoc} */
	@Override
	public String getProviderName() {
		return "chorda";
	}

}
