package com.teammoeg.chorda.dataholders.team;

import java.util.Collection;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.teammoeg.chorda.util.CDistHelper;

import net.minecraft.server.level.ServerPlayer;

public record SinglePlayerTeam(UUID player) implements AbstractTeam {

	@Override
	public Collection<ServerPlayer> getOnlineMembers() {
		ServerPlayer s =getPlayer();
		if(s!=null)
		ImmutableList.of(s);
		return ImmutableList.of();
	}
	public ServerPlayer getPlayer() {
		return CDistHelper.getServer().getPlayerList().getPlayer(player);
	}
	@Override
	public UUID getId() {
		return player;
	}

	@Override
	public String getName() {
		ServerPlayer s =getPlayer();
		if(s!=null)
			return s.getName().getString();
		return CDistHelper.getServer().getProfileCache().get(player).get().getName();
	}

	@Override
	public UUID getOwner() {
		return player;
	}

}
