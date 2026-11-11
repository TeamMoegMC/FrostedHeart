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

package com.teammoeg.frostedheart.content.town;

import java.util.List;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteDailyModel;
import com.teammoeg.frostedheart.content.town.buildings.institute.TownResearchSupplies;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.labor.LaborReport;
import com.teammoeg.frostedresearch.knowledge.labor.ResearchLaborPool;
import com.teammoeg.frostedresearch.knowledge.labor.ResearchSupplies;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * 城镇日结算中研究所人力与知识系统的耦合：读取当天各教育等级的研究人力，交给知识系统分配到研究需求（并从仓库扣除物品），
 * 剩余的合格人力尝试联想，结果写入研究所日报。
 * <p>
 * Couples a town day's research labor to the knowledge system: the day's {@link VirtualResourceType#RESEARCH_LABOR}
 * by education level goes to research requirements, which draw their daily items from the warehouse; the qualified
 * labor left over may associate; the outcome becomes the team's institute report. Runs after every building has
 * worked. A failure here is logged and does not stop the town day.
 */
public final class TownResearchSettlement {
    private TownResearchSettlement() {
    }

    static void settle(TeamTownData data, ServerLevel level, TeamDataHolder holder) {
        ResearchLaborPool pool = laborPool(data.resources);
        try {
            KnowledgeService knowledge = KnowledgeService.forTeam(holder, level.getGameTime());
            if (pool.total() <= TeamTownResourceHolder.DELTA) retireReport(knowledge, data.nextTownDay());
            else settle(pool, knowledge, new TownResearchSupplies(data.createTeamTown()), level.random, data.nextTownDay());
        } catch (RuntimeException failure) {
            FHMain.LOGGER.error("Town {} could not hand its research labor to the knowledge system", holder.getId(), failure);
        }
    }

    /**
     * 没有研究人力的清晨，把仍记着人力投入的日报换成一份空日报（只换一次），免得日志和研究所继续显示研究员停工前的分配。
     * <p>
     * A morning without research labor replaces a report that still shows labor with an empty one, once, so the journal
     * and the institute stop showing the allocations of the last day researchers worked.
     *
     * @return whether the report was replaced
     */
    public static boolean retireReport(KnowledgeService knowledge, long day) {
        var stale = knowledge.state().laborReport().filter(report -> report.totalLabor() > TeamTownResourceHolder.DELTA);
        if (stale.isEmpty()) return false;
        knowledge.recordLaborReport(new LaborReport(day, List.of(), List.of(), List.of(), LaborReport.Association.NONE));
        return true;
    }

    /**
     * The settlement itself, for tests and other callers that bring their own labor, knowledge and supplies.
     */
    public static LaborReport settle(ResearchLaborPool pool, KnowledgeService knowledge, ResearchSupplies supplies,
                                     RandomSource random, long day) {
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        List<Double> before = pool.byEducation();
        List<LaborReport.Allocation> allocations = knowledge.workLabor(pool, supplies);
        double associationLabor = pool.available(config.associationMinimumEducation.get());
        double chance = ResearchInstituteDailyModel.associationChance(associationLabor, config.associationRatePerStandardResearcherDay.get());
        LaborReport.Association association;
        if (associationLabor <= TeamTownResourceHolder.DELTA) {
            association = LaborReport.Association.NONE;
        } else if (random.nextDouble() >= chance) {
            association = new LaborReport.Association(LaborReport.Outcome.QUIET, associationLabor, chance, java.util.Optional.empty());
        } else {
            LaborReport.Association found = knowledge.instituteAssociation(random, day);
            association = new LaborReport.Association(found.outcome(), associationLabor, chance, found.subject());
        }
        LaborReport report = new LaborReport(day, before, allocations, pool.byEducation(), association);
        knowledge.recordLaborReport(report);
        return report;
    }

    static ResearchLaborPool laborPool(TeamTownResourceHolder resources) {
        ResearchLaborPool pool = new ResearchLaborPool();
        for (int education = 0; education <= VirtualResourceType.RESEARCH_LABOR.getMaxLevel(); education++)
            pool.add(education, resources.get(VirtualResourceType.RESEARCH_LABOR.generateAttribute(education)));
        return pool;
    }
}
