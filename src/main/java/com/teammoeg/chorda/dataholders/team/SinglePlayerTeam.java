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

/**
 * 单人模式下的团队实现。当没有 FTB Teams 模组时使用，每个玩家作为一个独立的团队。
 * <p>
 * Single-player team implementation. Used when FTB Teams mod is not present, where each player acts as an independent team.
 *
 * @param player 玩家的 UUID / the player's UUID
 */
public record SinglePlayerTeam(UUID player) implements AbstractTeam {

	/**
	 * {@inheritDoc}
	 * 返回该玩家（如果在线）的单元素列表。
	 * <p>
	 * Returns a singleton list of the player if online.
	 */
	@Override
	public Collection<ServerPlayer> getOnlineMembers() {
		ServerPlayer s =getPlayer();
		if(s!=null)
		ImmutableList.of(s);
		return ImmutableList.of();
	}
	/**
	 * 获取与此团队关联的服务器玩家实例。
	 * <p>
	 * Gets the server player instance associated with this team.
	 *
	 * @return 服务器玩家实例，如果玩家不在线则返回 null / the server player instance, or null if offline
	 */
	public ServerPlayer getPlayer() {
		return CDistHelper.getServer().getPlayerList().getPlayer(player);
	}
	/** {@inheritDoc} */
	@Override
	public UUID getId() {
		return player;
	}

	/**
	 * {@inheritDoc}
	 * 返回玩家的显示名称，如果不在线则从缓存中获取。
	 * <p>
	 * Returns the player's display name, or fetches from cache if offline.
	 */
	@Override
	public String getName() {
		ServerPlayer s =getPlayer();
		if(s!=null)
			return s.getName().getString();
		return CDistHelper.getServer().getProfileCache().get(player).get().getName();
	}

	/** {@inheritDoc} */
	@Override
	public UUID getOwner() {
		return player;
	}

}
