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

package com.teammoeg.frostedresearch.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Compatibility facade. New writes belong in {@link TeamResearchService}. */
public final class TeamResearchManager {
    private TeamResearchManager() {
    }

    public static TeamResearchService.GrantResult grantResult(ServerPlayer player, ResourceLocation resultId) {
        return TeamResearchService.grantResult(player, resultId);
    }

    public static TeamResearchService.RevokeResult revokeResult(ServerPlayer player, ResourceLocation resultId) {
        return TeamResearchService.revokeResult(player, resultId);
    }

    public static TeamResearchService.ResultInfo resultInfo(ServerPlayer player, ResourceLocation resultId) {
        return TeamResearchService.resultInfo(player, resultId);
    }
}
