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

package com.teammoeg.chorda.events;

import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * 当玩家所属的队伍发生变化时触发的事件。
 * 包含原始队伍、新队伍和受影响的玩家信息。
 * <p>
 * Event fired when a player's team membership changes.
 * Contains information about the original team, the new team, and the affected player.
 */
public class PlayerTeamChangedEvent extends Event{
	/** 玩家原来所属的队伍 / The player's original team */
	public final AbstractTeam originalTeam;
	/** 玩家新加入的队伍 / The player's new team */
	public final AbstractTeam team;
	/** 发生队伍变更的玩家 / The player whose team changed */
	public final ServerPlayer player;

	/**
	 * 创建玩家队伍变更事件。
	 * <p>
	 * Creates a player team changed event.
	 *
	 * @param originalTeam 原始队伍 / The original team
	 * @param team 新队伍 / The new team
	 * @param player 受影响的玩家 / The affected player
	 */
	public PlayerTeamChangedEvent( AbstractTeam originalTeam, AbstractTeam team, ServerPlayer player) {
		super();
		this.originalTeam = originalTeam;
		this.team = team;
		this.player = player;
	}




}
