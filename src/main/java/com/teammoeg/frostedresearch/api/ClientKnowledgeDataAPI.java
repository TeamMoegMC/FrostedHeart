/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.knowledge.KnowledgeProjection;
import com.teammoeg.frostedresearch.knowledge.TechnologyAccessProjection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client mirror installed atomically from the full knowledge snapshot packet. */
@OnlyIn(Dist.CLIENT)
public final class ClientKnowledgeDataAPI {
    private static volatile long catalogRevision;
    private static volatile KnowledgeProjection knowledgeProjection = KnowledgeProjection.EMPTY;
    private static volatile TechnologyAccessProjection technologyProjection = TechnologyAccessProjection.EMPTY;

    private ClientKnowledgeDataAPI() {
    }

    public static TeamDataClosure<TeamKnowledgeData> getData() {
        return CClientTeamDataManager.INSTANCE.getInstance().getDataHolder(FRSpecialDataTypes.KNOWLEDGE_DATA);
    }

    public static void install(long revision, KnowledgeProjection knowledge,
            TechnologyAccessProjection technology) {
        catalogRevision = revision;
        knowledgeProjection = knowledge;
        technologyProjection = technology;
    }

    public static long catalogRevision() {
        return catalogRevision;
    }

    public static KnowledgeProjection knowledgeProjection() {
        return knowledgeProjection;
    }

    public static TechnologyAccessProjection technologyProjection() {
        return technologyProjection;
    }
}
