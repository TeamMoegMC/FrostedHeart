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
