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

package com.teammoeg.chorda.compat.ftb;

import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.chorda.events.PlayerTeamChangedEvent;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.api.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.api.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public class FTBTeamsEvents {

    public static void init() {
        TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
        TeamEvent.CREATED.register(FTBTeamsEvents::syncDataWhenTeamCreated);
        TeamEvent.DELETED.register(FTBTeamsEvents::syncDataWhenTeamDeleted);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBTeamsEvents::syncDataWhenTeamTransfer);
        TeamsAPI.register(new FTBTeamsAPIProvider());
    }

    public static void syncDataWhenTeamChange(PlayerChangedTeamEvent event) {
    	 if (FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI() instanceof FTBTeamsAPIProvider) {
    		 FTBTeam oldteam=event.getPreviousTeam().map(FTBTeam::new).orElse(null);
    		 FTBTeam team=new FTBTeam(event.getTeam());
    		 
    		 MinecraftForge.EVENT_BUS.post(new PlayerTeamChangedEvent(oldteam,team, event.getPlayer()));
         }
    }

    public static void syncDataWhenTeamCreated(TeamCreatedEvent event) {
        if (FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI() instanceof FTBTeamsAPIProvider) {
            Team orig = FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(event.getCreator().getUUID()).orElse(null);
            AbstractTeam newTeam=new FTBTeam(event.getTeam());
            CTeamDataManager.INSTANCE.transfer(orig.getId(), newTeam);
            AbstractTeam oldTeam=new FTBTeam(orig);
            
            for(ServerPlayer p:event.getTeam().getOnlineMembers())
            	MinecraftForge.EVENT_BUS.post(new PlayerTeamChangedEvent(oldTeam,newTeam,p));
        }

    }

    public static void syncDataWhenTeamDeleted(TeamEvent event) {
        if (FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI() instanceof FTBTeamsAPIProvider) {
            UUID owner = event.getTeam().getOwner();
            Team orig = FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(owner).orElse(null);
            AbstractTeam newTeam=new FTBTeam(orig);
            System.out.println("origin team:"+event.getTeam().getId()+",new team:"+orig.getId());
            CTeamDataManager.INSTANCE.transfer(event.getTeam().getId(), newTeam);
            
            AbstractTeam oldTeam=new FTBTeam(event.getTeam());
            for(ServerPlayer p:orig.getOnlineMembers())
            	MinecraftForge.EVENT_BUS.post(new PlayerTeamChangedEvent(oldTeam,newTeam,p));
            
        }

    }

    public static void syncDataWhenTeamTransfer(PlayerTransferredTeamOwnershipEvent event) {
    	 if (FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI() instanceof FTBTeamsAPIProvider) {

             CTeamDataManager.INSTANCE.get(event.getTeam().getId()).setOwnerName(event.getFrom().getGameProfile().getName());
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
