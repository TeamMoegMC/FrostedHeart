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

package com.teammoeg.frostedheart.content.town.buildings.institute;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedresearch.knowledge.KnowledgeCapacity;

import net.minecraft.resources.ResourceLocation;

/**
 * 研究所向知识系统登记的容量扩展：队伍每座可工作研究所的书架提供档案与来件观察容量。
 * <p>
 * The research institutes' knowledge capacity provider: the bookshelves of each working institute of a team add
 * archive and inbox observation capacity.
 */
public final class InstituteKnowledgeCapacity {
    public static final ResourceLocation ID = FHMain.rl("research_institute");

    private InstituteKnowledgeCapacity() {
    }

    public static void register() {
        KnowledgeCapacity.register(ID, InstituteKnowledgeCapacity::extension);
    }

    /** Reads saved building state only; a team without town data grants nothing. */
    public static KnowledgeCapacity.Extension extension(TeamDataHolder team) {
        TeamTownData town = team.getOptional(FHSpecialDataTypes.TOWN_DATA).orElse(null);
        if (town == null) return KnowledgeCapacity.Extension.NONE;
        long archive = 0, inbox = 0;
        for (AbstractTownBuilding building : town.getBuildingStore().resolvedMap().values()) {
            if (building instanceof ResearchInstituteBuilding institute) {
                archive += institute.archiveCapacity();
                inbox += institute.inboxCapacity();
            }
        }
        return new KnowledgeCapacity.Extension((int) Math.min(Integer.MAX_VALUE, archive), (int) Math.min(Integer.MAX_VALUE, inbox));
    }
}
