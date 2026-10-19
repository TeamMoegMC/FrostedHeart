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
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import net.minecraft.nbt.CompoundTag;

/**
 * Atomic visible state sent after login, team changes, grants and catalogue reloads. The persisted team state stays on
 * the server; {@code archive_view} carries everything the client displays.
 */
public record KnowledgeSyncSnapshot(long catalogRevision, KnowledgeProjection knowledge,
                                    TechnologyAccessProjection technology, CompoundTag archiveView) {
    public static final Codec<KnowledgeSyncSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("catalog_revision").forGetter(KnowledgeSyncSnapshot::catalogRevision),
            KnowledgeProjection.CODEC.fieldOf("knowledge").forGetter(KnowledgeSyncSnapshot::knowledge),
            TechnologyAccessProjection.CODEC.fieldOf("technology").forGetter(KnowledgeSyncSnapshot::technology),
            CompoundTag.CODEC.fieldOf("archive_view").forGetter(KnowledgeSyncSnapshot::archiveView)
    ).apply(instance, KnowledgeSyncSnapshot::new));

    public static KnowledgeSyncSnapshot create(TeamDataHolder team) {
        return new KnowledgeSyncSnapshot(ResearchResultCatalog.current().revision(),
                TechnologyAccessResolver.projectKnowledge(team.getData(FRSpecialDataTypes.KNOWLEDGE_DATA)),
                TechnologyAccessResolver.project(team), KnowledgeService.forTeam(team, 0).snapshot());
    }
}
