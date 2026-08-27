/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.knowledge.KnowledgeProjection;
import com.teammoeg.frostedresearch.knowledge.KnowledgeLabProjection;
import com.teammoeg.frostedresearch.knowledge.TechnologyAccessProjection;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientKnowledgeDataAPITest {
    @AfterEach
    void tearDown() {
        ClientKnowledgeDataAPI.reset();
    }

    @Test
    void resetClearsInstalledProjectionAndTeamMirror() {
        ResourceLocation finding = id("finding");
        ResourceLocation recipe = id("recipe");
        TeamKnowledgeData teamData = new TeamKnowledgeData();
        teamData.acquireFinding(finding);
        CClientTeamDataManager.INSTANCE.getInstance().setData(
                FRSpecialDataTypes.KNOWLEDGE_DATA, teamData);

        KnowledgeProjection knowledge = new KnowledgeProjection(List.of());
        TechnologyAccessProjection technology = TechnologyAccessProjection.create(
                Set.of(recipe), Set.of(), Set.of(), Map.of(), Map.of(), Map.of());
        ClientKnowledgeDataAPI.install(17, knowledge,
                com.teammoeg.frostedresearch.knowledge.KnowledgeLabProjection.EMPTY, technology);

        assertEquals(17, ClientKnowledgeDataAPI.catalogRevision());
        assertSame(knowledge, ClientKnowledgeDataAPI.knowledgeProjection());
        assertTrue(ClientKnowledgeDataAPI.technologyProjection().managedRecipes().contains(recipe));
        assertTrue(ClientKnowledgeDataAPI.getData().get().hasFinding(finding));

        ClientKnowledgeDataAPI.reset();

        assertEquals(0, ClientKnowledgeDataAPI.catalogRevision());
        assertSame(KnowledgeProjection.EMPTY, ClientKnowledgeDataAPI.knowledgeProjection());
        assertSame(KnowledgeLabProjection.EMPTY, ClientKnowledgeDataAPI.knowledgeLabProjection());
        assertSame(TechnologyAccessProjection.EMPTY, ClientKnowledgeDataAPI.technologyProjection());
        assertFalse(ClientKnowledgeDataAPI.getData().get().hasFinding(finding));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
