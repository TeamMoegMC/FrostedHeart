package com.teammoeg.frostedheart.events;

import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.network.FHResearchDataSyncPacket;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.event.PlayerChangedTeamEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public class FTBTeamsEvents {

	public FTBTeamsEvents() {
	}
	public static void syncDataWhenTeamChange(PlayerChangedTeamEvent event) {
    	PacketHandler.send(PacketDistributor.PLAYER.with(() -> event.getPlayer()),
                new FHResearchDataSyncPacket(
                        FTBTeamsAPI.getPlayerTeam(event.getPlayer()).getId()));
    }
}
