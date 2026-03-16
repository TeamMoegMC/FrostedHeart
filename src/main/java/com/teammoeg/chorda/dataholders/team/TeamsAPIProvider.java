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
 * 团队 API 的提供者接口。不同的团队系统（如 FTB Teams、单人模式）通过实现此接口来提供团队功能。
 * <p>
 * Provider interface for the Teams API. Different team systems (e.g., FTB Teams, single-player mode)
 * implement this interface to provide team functionality.
 */
public interface TeamsAPIProvider {
	/**
	 * 根据服务器玩家获取其所属的团队。
	 * <p>
	 * Gets the team that a server player belongs to.
	 *
	 * @param p 服务器玩家 / the server player
	 * @return 玩家所属的团队 / the team the player belongs to
	 */
	AbstractTeam getTeamByPlayer(ServerPlayer p);

	/**
	 * 根据 UUID 获取团队。
	 * <p>
	 * Gets a team by its UUID.
	 *
	 * @param uuid 团队的 UUID / the team's UUID
	 * @return 对应的团队 / the corresponding team
	 */
	AbstractTeam getTeamByUuid(UUID uuid);

	/**
	 * 获取此提供者的名称标识。
	 * <p>
	 * Gets the name identifier of this provider.
	 *
	 * @return 提供者名称 / the provider name
	 */
	String getProviderName();
}
