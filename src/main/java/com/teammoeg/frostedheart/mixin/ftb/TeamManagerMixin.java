package com.teammoeg.frostedheart.mixin.ftb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.FTBFixUtils;

import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import dev.ftb.mods.ftbteams.net.SyncTeamsMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

@Mixin(TeamManager.class)
public abstract class TeamManagerMixin {
	@Inject(at=@At("HEAD"),method="sync(Lnet/minecraft/entity/player/ServerPlayerEntity;Ldev/ftb/mods/ftbteams/data/Team;)V", remap = false)
	public void fh$sync(ServerPlayerEntity player, Team self,CallbackInfo cbi) {
		FTBFixUtils.networkPlayer=player;
	}
	@Shadow(remap=false)
	public abstract void save();
	
	@Shadow(remap=false)
	MinecraftServer server;
	
	@Shadow(remap=false)
	public abstract ClientTeamManager createClientTeamManager();
	@Shadow(remap=false)
	public abstract Team getPlayerTeam(ServerPlayerEntity player);
	@Overwrite(remap=false)
	public void syncAll() {
		save();

		ClientTeamManager clientManager = createClientTeamManager();

		for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
			FTBFixUtils.networkPlayer=player;
			new SyncTeamsMessage(clientManager, getPlayerTeam(player)).sendTo(player);
			server.getPlayerList().updatePermissionLevel(player);
		}
	}
}
