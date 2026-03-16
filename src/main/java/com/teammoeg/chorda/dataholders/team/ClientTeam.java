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

/**
 * 客户端侧的团队实现，用于客户端数据同步时的占位团队。
 * 使用当前客户端玩家的信息作为团队属性，在线成员列表始终为空。
 * <p>
 * Client-side team implementation, used as a placeholder team for client data synchronization.
 * Uses the current client player's information as team properties; the online members list is always empty.
 */
public class ClientTeam implements AbstractTeam {

	/**
	 * 构造一个新的客户端团队实例。
	 * <p>
	 * Constructs a new client team instance.
	 */
	public ClientTeam() {

	}

	/**
	 * {@inheritDoc}
	 * 客户端侧始终返回空列表。
	 * <p>
	 * Always returns an empty list on the client side.
	 */
	@Override
	public Collection<ServerPlayer> getOnlineMembers() {
		return ImmutableList.of();
	}
	UUID ZERO=UUID.fromString("11da5049-661a-4435-9742-6ba376291d5b");

	/** {@inheritDoc} */
	@Override
	public UUID getId() {
		return ZERO;
	}

	/**
	 * {@inheritDoc}
	 * 返回当前客户端玩家的名称。
	 * <p>
	 * Returns the current client player's name.
	 */
	@Override
	public String getName() {
		return ClientUtils.getMc().player.getName().getString();
	}

	/**
	 * {@inheritDoc}
	 * 返回当前客户端玩家的 UUID。
	 * <p>
	 * Returns the current client player's UUID.
	 */
	@Override
	public UUID getOwner() {
		return ClientUtils.getMc().player.getUUID();
	}

}
