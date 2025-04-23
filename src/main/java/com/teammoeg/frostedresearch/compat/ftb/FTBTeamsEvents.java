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

package com.teammoeg.frostedresearch.compat.ftb;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedresearch.network.FHResearchDataSyncPacket;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.api.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.server.level.ServerPlayer;

public class FTBTeamsEvents {

    public static void init() {
        //TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
        //TeamEvent.CREATED.register(FTBTeamsEvents::syncDataWhenTeamCreated);
        //TeamEvent.DELETED.register(FTBTeamsEvents::syncDataWhenTeamDeleted);
        //TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBTeamsEvents::syncDataWhenTeamTransfer);
    }

    public static void syncDataWhenTeamChange(PlayerChangedTeamEvent event) {
    	if(FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI().getProviderName().equals("ftbteams")) {
	    	if(event.getPlayer()!=null)
	    		FRNetwork.INSTANCE.sendPlayer(event.getPlayer(),
	                new FHResearchDataSyncPacket(ResearchDataAPI.getData(event.getPlayer()).get()));
    	}
    }

    public static void syncDataWhenTeamCreated(TeamCreatedEvent event) {
        if (FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI().getProviderName().equals("ftbteams")) {
            Team orig = FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(event.getCreator().getUUID()).orElse(null);

            for(ServerPlayer p:event.getTeam().getOnlineMembers()) {
            	FRNetwork.INSTANCE.sendPlayer(p,
	                    new FHResearchDataSyncPacket(ResearchDataAPI.getData(p).get()));
            }
        }

    }

    public static void syncDataWhenTeamDeleted(TeamEvent event) {
        if (FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI().getProviderName().equals("ftbteams")) {
            for(ServerPlayer p:event.getTeam().getOnlineMembers()) {
            	FRNetwork.INSTANCE.sendPlayer(p,
	                    new FHResearchDataSyncPacket(ResearchDataAPI.getData(p).get()));
            }
            
        }

    }

    public static void syncDataWhenTeamTransfer(PlayerTransferredTeamOwnershipEvent event) {
       

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
