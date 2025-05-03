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
