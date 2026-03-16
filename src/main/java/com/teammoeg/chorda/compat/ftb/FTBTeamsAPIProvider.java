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

import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.TeamsAPIProvider;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;

/**
 * 基于FTB Teams模组的队伍API提供者实现。
 * 通过FTB Teams API来查询和管理队伍信息。
 * <p>
 * Teams API provider implementation based on the FTB Teams mod.
 * Queries and manages team information through the FTB Teams API.
 */
public class FTBTeamsAPIProvider implements TeamsAPIProvider {

	/**
	 * 创建FTB Teams API提供者实例。
	 * <p>
	 * Creates an FTB Teams API provider instance.
	 */
	public FTBTeamsAPIProvider() {
	}

	/**
	 * 通过玩家获取其所属的队伍。
	 * <p>
	 * Gets the team that a player belongs to.
	 *
	 * @param player 服务端玩家实例 / The server player instance
	 * @return 玩家所属的队伍，如果不存在则返回null / The player's team, or null if none exists
	 */
	@Override
	public AbstractTeam getTeamByPlayer(ServerPlayer player) {
		return FTBTeamsAPI.api().getManager().getTeamForPlayer(player).map(FTBTeam::new).orElse(null);
	}

	/**
	 * 通过UUID获取队伍。
	 * <p>
	 * Gets a team by its UUID.
	 *
	 * @param uuid 队伍的唯一标识符 / The unique identifier of the team
	 * @return 对应的队伍，如果不存在则返回null / The corresponding team, or null if none exists
	 */
	@Override
	public AbstractTeam getTeamByUuid(UUID uuid) {
		return FTBTeamsAPI.api().getManager().getTeamByID(uuid).map(FTBTeam::new).orElse(null);
	}

	/**
	 * 获取此API提供者的名称。
	 * <p>
	 * Gets the name of this API provider.
	 *
	 * @return 提供者名称 "ftbteams" / The provider name "ftbteams"
	 */
	@Override
	public String getProviderName() {
		return "ftbteams";
	}

}
