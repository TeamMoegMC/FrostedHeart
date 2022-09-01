package com.teammoeg.frostedheart.research.api;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;

public class ResearchDataAPI {

    private ResearchDataAPI() {
    }

    public static TeamResearchData getData(ServerPlayerEntity id) {
        return FHResearchDataManager.INSTANCE.getData(FTBTeamsAPI.getPlayerTeam(id).getId());

    }

    public static TeamResearchData getData(UUID id) {
        return FHResearchDataManager.INSTANCE.getData(id);

    }

    public static CompoundNBT getVariants(ServerPlayerEntity id) {
        return FHResearchDataManager.INSTANCE.getData(FTBTeamsAPI.getPlayerTeam(id).getId()).getVariants();

    }

    public static CompoundNBT getVariants(UUID id) {
        return FHResearchDataManager.INSTANCE.getData(id).getVariants();

    }
}
