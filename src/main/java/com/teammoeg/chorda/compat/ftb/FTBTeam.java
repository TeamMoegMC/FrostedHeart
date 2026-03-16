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

/**
 * FTB Teams队伍的AbstractTeam适配器实现。
 * 将FTB Teams的Team接口包装为Chorda的AbstractTeam接口。
 * <p>
 * AbstractTeam adapter implementation for FTB Teams.
 * Wraps the FTB Teams Team interface as Chorda's AbstractTeam interface.
 *
 * @param team FTB Teams队伍实例 / The FTB Teams team instance
 */
public record FTBTeam(Team team) implements AbstractTeam {
	/**
	 * 获取当前在线的队伍成员。
	 * <p>
	 * Gets the currently online team members.
	 *
	 * @return 在线成员集合 / The collection of online members
	 */
	@Override
	public Collection<ServerPlayer> getOnlineMembers() {
		return team.getOnlineMembers();
	}
	/**
	 * 获取队伍的唯一标识符。
	 * <p>
	 * Gets the unique identifier of the team.
	 *
	 * @return 队伍UUID / The team UUID
	 */
	@Override
	public UUID getId() {
		return team.getId();
	}
	/**
	 * 获取队伍名称的字符串表示。
	 * <p>
	 * Gets the string representation of the team name.
	 *
	 * @return 队伍名称 / The team name
	 */
	@Override
	public String getName() {

		return team.getName().getString();
	}
	/**
	 * 获取队伍所有者的UUID。
	 * <p>
	 * Gets the UUID of the team owner.
	 *
	 * @return 队伍所有者UUID / The team owner UUID
	 */
	@Override
	public UUID getOwner() {
		return team.getOwner();
	}

}
