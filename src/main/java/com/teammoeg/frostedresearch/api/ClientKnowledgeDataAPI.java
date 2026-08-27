/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.knowledge.KnowledgeProjection;
import com.teammoeg.frostedresearch.knowledge.KnowledgeLabProjection;
import com.teammoeg.frostedresearch.knowledge.TechnologyAccessProjection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client mirror installed atomically from the full knowledge snapshot packet. */
@OnlyIn(Dist.CLIENT)
public final class ClientKnowledgeDataAPI {
    private static final ClientSnapshot EMPTY = new ClientSnapshot(
            0, KnowledgeProjection.EMPTY, KnowledgeLabProjection.EMPTY, TechnologyAccessProjection.EMPTY);
    private static volatile ClientSnapshot current = EMPTY;

    private ClientKnowledgeDataAPI() {
    }

    public static TeamDataClosure<TeamKnowledgeData> getData() {
        return CClientTeamDataManager.INSTANCE.getInstance().getDataHolder(FRSpecialDataTypes.KNOWLEDGE_DATA);
    }

    public static void install(long revision, KnowledgeProjection knowledge, KnowledgeLabProjection laboratory,
            TechnologyAccessProjection technology) {
        current = new ClientSnapshot(revision, knowledge, laboratory, technology);
    }

    /** Clears both the public projections and the client-side team knowledge mirror. */
    public static void reset() {
        current = EMPTY;
        CClientTeamDataManager.INSTANCE.getInstance().setData(
                FRSpecialDataTypes.KNOWLEDGE_DATA, new TeamKnowledgeData());
    }

    public static long catalogRevision() {
        return current.catalogRevision();
    }

    public static KnowledgeProjection knowledgeProjection() {
        return current.knowledgeProjection();
    }

    public static KnowledgeLabProjection knowledgeLabProjection() {
        return current.knowledgeLabProjection();
    }

    public static TechnologyAccessProjection technologyProjection() {
        return current.technologyProjection();
    }

    private record ClientSnapshot(long catalogRevision, KnowledgeProjection knowledgeProjection,
            KnowledgeLabProjection knowledgeLabProjection,
            TechnologyAccessProjection technologyProjection) {
    }
}
