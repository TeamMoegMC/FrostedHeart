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

import net.minecraft.server.level.ServerPlayer;

/**
 * 团队的抽象接口，定义了团队的基本属性和操作。
 * 作为 FTB Teams 集成和单人模式团队的通用抽象层。
 * <p>
 * Abstract interface for teams, defining basic team properties and operations.
 * Serves as a common abstraction layer for FTB Teams integration and single-player teams.
 */
public interface AbstractTeam {
	/**
	 * 获取当前在线的团队成员。
	 * <p>
	 * Gets all currently online team members.
	 *
	 * @return 在线成员集合 / the collection of online members
	 */
	Collection<ServerPlayer> getOnlineMembers();

	/**
	 * 获取团队的唯一标识符。
	 * <p>
	 * Gets the unique identifier of the team.
	 *
	 * @return 团队 UUID / the team UUID
	 */
	UUID getId();

	/**
	 * 获取团队名称。
	 * <p>
	 * Gets the team name.
	 *
	 * @return 团队名称 / the team name
	 */
	String getName();

	/**
	 * 获取团队所有者的 UUID。
	 * <p>
	 * Gets the UUID of the team owner.
	 *
	 * @return 所有者 UUID / the owner's UUID
	 */
	UUID getOwner();
}
