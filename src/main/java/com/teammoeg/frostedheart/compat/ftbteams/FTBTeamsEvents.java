/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.compat.ftbteams;

import java.util.UUID;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.api.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.server.level.ServerPlayer;

public class FTBTeamsEvents {

    public static void init() {
        TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
        TeamEvent.CREATED.register(FTBTeamsEvents::syncDataWhenTeamCreated);
        TeamEvent.DELETED.register(FTBTeamsEvents::syncDataWhenTeamDeleted);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBTeamsEvents::syncDataWhenTeamTransfer);
    }

    public static void syncDataWhenTeamChange(PlayerChangedTeamEvent event) {
    	if(event.getPlayer()!=null)
    		FHNetwork.sendPlayer(event.getPlayer(),
                new FHResearchDataSyncPacket(ResearchDataAPI.getData(event.getPlayer()).get()));
    }

    public static void syncDataWhenTeamCreated(TeamCreatedEvent event) {
        if (FTBTeamsAPI.api().isManagerLoaded()) {
            Team orig = FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(event.getCreator().getUUID()).orElse(null);

            CTeamDataManager.INSTANCE.transfer(orig.getId(), event.getTeam());
            for(ServerPlayer p:event.getTeam().getOnlineMembers()) {
	            FHNetwork.sendPlayer(p,
	                    new FHResearchDataSyncPacket(ResearchDataAPI.getData(p).get()));
            }
        }

    }

    public static void syncDataWhenTeamDeleted(TeamEvent event) {
        if (FTBTeamsAPI.api().isManagerLoaded()) {
            UUID owner = event.getTeam().getOwner();
            Team orig = FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(owner).orElse(null);

            CTeamDataManager.INSTANCE.transfer(event.getTeam().getId(), orig);
            for(ServerPlayer p:event.getTeam().getOnlineMembers()) {
	            FHNetwork.sendPlayer(p,
	                    new FHResearchDataSyncPacket(ResearchDataAPI.getData(p).get()));
            }
            
        }

    }

    public static void syncDataWhenTeamTransfer(PlayerTransferredTeamOwnershipEvent event) {
        if (FTBTeamsAPI.api().isManagerLoaded()) {

            CTeamDataManager.INSTANCE.get(event.getTeam()).setOwnerName(event.getFrom().getGameProfile().getName());
        }

    }
   /* public static void syncDataWhenOwnerLeft(PlayerLeftPartyTeamEvent event) {
    	 if (FTBTeamsAPI.isManagerLoaded()) {
    		 if(event.getPlayerId().equals(event.getTeam().getOwner())) {//transfer to last player
	             UUID owner = event.getTeam().getOwner();
	             PlayerTeam orig = FTBTeamsAPI.getManager().getInternalPlayerTeam(owner);
	
	             FHResearchDataManager.INSTANCE.transfer(event.getTeam().getId(), orig);
    		 }
         }
    	
    }*/
    public FTBTeamsEvents() {
    }
}
