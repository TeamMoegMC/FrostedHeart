/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeLabProjectionTest {
    @Test
    void everyPersistentResultSetRemainsVisibleWhenDefinitionsDisappear() {
        TeamKnowledgeData data = new TeamKnowledgeData();
        data.acquireFinding(id("finding"));
        data.acquireDesign(id("design"));
        data.acquireConstruction(id("construction"));
        data.acquireProcedure(id("procedure"));

        KnowledgeProjection ambient = new KnowledgeProjection(java.util.List.of());
        KnowledgeLabProjection projection = TechnologyAccessResolver.projectKnowledgeLab(data, ambient);

        assertEquals(4, projection.results().size());
        assertTrue(projection.results().stream().allMatch(KnowledgeLabProjection.ResultSummary::orphan));
        assertEquals(Set.of(ResearchResult.ResultType.FINDING, ResearchResult.ResultType.DESIGN,
                        ResearchResult.ResultType.CONSTRUCTION, ResearchResult.ResultType.PROCEDURE),
                projection.results().stream().map(KnowledgeLabProjection.ResultSummary::type)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("knowledge_lab_test", path);
    }
}
