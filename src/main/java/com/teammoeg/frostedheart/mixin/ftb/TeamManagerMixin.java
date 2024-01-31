/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.ftb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.mixin.FTBFixUtils;

import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import dev.ftb.mods.ftbteams.net.SyncTeamsMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

@Mixin(TeamManager.class)
public abstract class TeamManagerMixin {
    @Shadow(remap = false)
    MinecraftServer server;

    @Shadow(remap = false)
    public abstract ClientTeamManager createClientTeamManager();

    @Inject(at = @At("HEAD"), method = "sync(Lnet/minecraft/entity/player/ServerPlayerEntity;Ldev/ftb/mods/ftbteams/data/Team;)V", remap = false)
    public void fh$sync(ServerPlayerEntity player, Team self, CallbackInfo cbi) {
        FTBFixUtils.networkPlayer = player;
    }

    @Shadow(remap = false)
    public abstract Team getPlayerTeam(ServerPlayerEntity player);

    @Shadow(remap = false)
    public abstract void save();

    /**
     * @author khjxiao
     * @reason TODO
     */
    @Overwrite(remap = false)
    public void syncAll() {
        save();

        ClientTeamManager clientManager = createClientTeamManager();

        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            FTBFixUtils.networkPlayer = player;
            new SyncTeamsMessage(clientManager, getPlayerTeam(player)).sendTo(player);
            server.getPlayerList().updatePermissionLevel(player);
        }
    }
}
