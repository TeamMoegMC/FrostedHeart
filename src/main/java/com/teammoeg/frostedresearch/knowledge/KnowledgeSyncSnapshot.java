/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;

/** Atomic full state sent after login, team changes, grants and catalogue reloads. */
public record KnowledgeSyncSnapshot(long catalogRevision, TeamKnowledgeData teamData,
                                    KnowledgeProjection knowledge, TechnologyAccessProjection technology,
                                    net.minecraft.nbt.CompoundTag archiveView) {
    public static final Codec<KnowledgeSyncSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("catalog_revision").forGetter(KnowledgeSyncSnapshot::catalogRevision),
            TeamKnowledgeData.CODEC.fieldOf("team_data").forGetter(KnowledgeSyncSnapshot::teamData),
            KnowledgeProjection.CODEC.fieldOf("knowledge").forGetter(KnowledgeSyncSnapshot::knowledge),
            TechnologyAccessProjection.CODEC.fieldOf("technology").forGetter(KnowledgeSyncSnapshot::technology),
            net.minecraft.nbt.CompoundTag.CODEC.fieldOf("archive_view").forGetter(KnowledgeSyncSnapshot::archiveView)
    ).apply(instance, KnowledgeSyncSnapshot::new));

    public KnowledgeSyncSnapshot(long revision, TeamKnowledgeData data, KnowledgeProjection knowledge, TechnologyAccessProjection technology) {
        this(revision, data, knowledge, technology, new net.minecraft.nbt.CompoundTag());
    }
    public static KnowledgeSyncSnapshot create(TeamDataHolder team) {
        TeamKnowledgeData data = team.getData(FRSpecialDataTypes.KNOWLEDGE_DATA);
        return new KnowledgeSyncSnapshot(ResearchResultCatalog.current().revision(), data.copy(),
                TechnologyAccessResolver.projectKnowledge(data), TechnologyAccessResolver.project(team),
                KnowledgeService.forTeam(team, 0).snapshot());
    }
}
