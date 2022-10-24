/*
 * Copyright (c) 2022 TeamMoeg
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

    public static boolean isResearchComplete(ServerPlayerEntity id,String research) {
        return FHResearchDataManager.INSTANCE.getData(FTBTeamsAPI.getPlayerTeam(id).getId()).getData(research).isCompleted();

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
