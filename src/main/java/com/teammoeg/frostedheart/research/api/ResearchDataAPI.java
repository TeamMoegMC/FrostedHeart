package com.teammoeg.frostedheart.research.api;

import java.util.UUID;

import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.research.TeamResearchData;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.entity.player.ServerPlayerEntity;

public class ResearchDataAPI {

	private ResearchDataAPI() {
	}
	public static TeamResearchData getData(ServerPlayerEntity id) {
		return ResearchDataManager.INSTANCE.getData(FTBTeamsAPI.getPlayerTeam(id).getId());

	}
	public static TeamResearchData getData(UUID id) {
		return ResearchDataManager.INSTANCE.getData(id);

	}
}
