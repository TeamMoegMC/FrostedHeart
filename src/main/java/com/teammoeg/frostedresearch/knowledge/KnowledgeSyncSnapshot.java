/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;

/** Atomic full state sent after login, team changes, grants and catalogue reloads. */
public record KnowledgeSyncSnapshot(long catalogRevision, TeamKnowledgeData teamData,
        KnowledgeProjection knowledge, KnowledgeLabProjection laboratory,
        TechnologyAccessProjection technology) {
    public static final Codec<KnowledgeSyncSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("catalog_revision").forGetter(KnowledgeSyncSnapshot::catalogRevision),
            TeamKnowledgeData.NETWORK_CODEC.fieldOf("team_data").forGetter(KnowledgeSyncSnapshot::teamData),
            KnowledgeProjection.CODEC.fieldOf("knowledge").forGetter(KnowledgeSyncSnapshot::knowledge),
            KnowledgeLabProjection.CODEC.optionalFieldOf("laboratory", KnowledgeLabProjection.EMPTY)
                    .forGetter(KnowledgeSyncSnapshot::laboratory),
            TechnologyAccessProjection.CODEC.fieldOf("technology").forGetter(KnowledgeSyncSnapshot::technology)
    ).apply(instance, KnowledgeSyncSnapshot::new));

    public static KnowledgeSyncSnapshot create(TeamDataHolder team) {
        TeamKnowledgeData data = team.getData(FRSpecialDataTypes.KNOWLEDGE_DATA);
        KnowledgeProjection knowledge = TechnologyAccessResolver.projectKnowledge(data);
        return new KnowledgeSyncSnapshot(ResearchResultCatalog.current().revision(), data.copy(), knowledge,
                TechnologyAccessResolver.projectKnowledgeLab(data, knowledge), TechnologyAccessResolver.project(team));
    }
}
