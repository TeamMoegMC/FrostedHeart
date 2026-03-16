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

/**
 * FTB Teams事件监听器和桥接类。
 * 监听FTB Teams的队伍事件，并将其转换为Chorda的事件系统，
 * 以实现队伍数据的同步和迁移。
 * <p>
 * FTB Teams event listener and bridge class.
 * Listens to FTB Teams team events and translates them into Chorda's event system,
 * enabling team data synchronization and migration.
 */
public class FTBTeamsEvents {

    /**
     * 初始化FTB Teams事件监听器并注册FTB Teams API提供者。
     * 注册队伍变更、创建、删除和所有权转移的事件处理器。
     * <p>
     * Initializes FTB Teams event listeners and registers the FTB Teams API provider.
     * Registers event handlers for team changes, creation, deletion, and ownership transfers.
     */
    public static void init() {
        TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
        TeamEvent.CREATED.register(FTBTeamsEvents::syncDataWhenTeamCreated);
        TeamEvent.DELETED.register(FTBTeamsEvents::syncDataWhenTeamDeleted);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBTeamsEvents::syncDataWhenTeamTransfer);
        TeamsAPI.register(new FTBTeamsAPIProvider());
    }

    /**
     * 当玩家更换队伍时同步数据。将FTB Teams事件转发为Chorda的PlayerTeamChangedEvent。
     * <p>
     * Synchronizes data when a player changes teams. Forwards the FTB Teams event
     * as a Chorda PlayerTeamChangedEvent.
     *
     * @param event FTB Teams玩家队伍变更事件 / The FTB Teams player changed team event
     */
    public static void syncDataWhenTeamChange(PlayerChangedTeamEvent event) {
    	 if (FTBTeamsAPI.api().isManagerLoaded()&&TeamsAPI.getAPI() instanceof FTBTeamsAPIProvider) {
    		 FTBTeam oldteam=event.getPreviousTeam().map(FTBTeam::new).orElse(null);
    		 FTBTeam team=new FTBTeam(event.getTeam());
    		 
    		 MinecraftForge.EVENT_BUS.post(new PlayerTeamChangedEvent(oldteam,team, event.getPlayer()));
         }
    }

    /**
     * 当队伍被创建时同步数据。将原有队伍的数据迁移到新队伍，
     * 并为所有在线成员触发PlayerTeamChangedEvent。
     * <p>
     * Synchronizes data when a team is created. Transfers data from the original team
     * to the new team and fires PlayerTeamChangedEvent for all online members.
     *
     * @param event FTB Teams队伍创建事件 / The FTB Teams team created event
     */
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

    /**
     * 当队伍被删除时同步数据。将被删除队伍的数据迁移到队长的个人队伍，
     * 并为所有在线成员触发PlayerTeamChangedEvent。
     * <p>
     * Synchronizes data when a team is deleted. Transfers data from the deleted team
     * to the owner's personal team and fires PlayerTeamChangedEvent for all online members.
     *
     * @param event FTB Teams队伍事件 / The FTB Teams team event
     */
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

    /**
     * 当队伍所有权转移时同步数据。更新队伍数据中的所有者名称。
     * <p>
     * Synchronizes data when team ownership is transferred. Updates the owner name
     * in the team data.
     *
     * @param event FTB Teams所有权转移事件 / The FTB Teams ownership transferred event
     */
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
    /**
     * 创建FTBTeamsEvents实例。
     * <p>
     * Creates an FTBTeamsEvents instance.
     */
    public FTBTeamsEvents() {
    }
}
