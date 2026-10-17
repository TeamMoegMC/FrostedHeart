/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/** Lookup boundary for the independent V2 team knowledge component. */
public final class KnowledgeDataAPI {
    private KnowledgeDataAPI() {
    }

    public static TeamDataClosure<TeamKnowledgeData> getData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return CTeamDataManager.INSTANCE.get(TeamsAPI.getAPI().getTeamByPlayer(serverPlayer))
                    .getDataHolder(FRSpecialDataTypes.KNOWLEDGE_DATA);
        }
        return ClientKnowledgeDataAPI.getData();
    }

    public static Optional<TeamDataClosure<TeamKnowledgeData>> getData(UUID teamId) {
        TeamDataHolder holder = CTeamDataManager.INSTANCE.get(teamId);
        if (holder == null) return Optional.empty();
        return Optional.of(holder.getDataHolder(FRSpecialDataTypes.KNOWLEDGE_DATA));
    }
}
