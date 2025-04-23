package com.teammoeg.chorda.dataholders.team;

import java.util.Collection;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public interface AbstractTeam {
	Collection<ServerPlayer> getOnlineMembers();

	UUID getId();

	String getName();

	UUID getOwner();
}
