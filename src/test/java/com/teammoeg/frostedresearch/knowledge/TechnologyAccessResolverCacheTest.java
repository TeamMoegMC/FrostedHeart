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

package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.chorda.dataholders.team.ClientTeam;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnologyAccessResolverCacheTest {
    private static final ResourceLocation RECIPE = id("recipe");
    private static final ResourceLocation DESIGN = id("design");

    @BeforeAll
    static void bootstrapMinecraft() {
        CUtils.startTestEnvironment();
    }

    @BeforeEach
    void installInputs() {
        // ResearchCommonEvents.makeUnlocks registers these in game; JUnit does not subscribe it.
        ResearchHooks.locklists.put(ResearchHooks.RECIPE_UNLOCK_LIST, new ResearchHooks.RecipeUnlockList());
        ResearchHooks.locklists.put(ResearchHooks.MULTIBLOCK_UNLOCK_LIST, new ResearchHooks.MultiblockUnlockList());
        ResearchHooks.locklists.put(ResearchHooks.BLOCK_UNLOCK_LIST, new ResearchHooks.BlockUnlockList());
        installCatalog();
    }

    @AfterEach
    void clear() {
        TeamResearchService.clear();
        ResearchHooks.locklists.clear();
        ResearchResultCatalog.clearForTests();
        KnowledgeDefinitions.clearForTests();
    }

    @Test
    void teamProjectionIsReusedUntilItsKnowledgeChanges() {
        TeamDataHolder team = team();
        TechnologyAccessProjection locked = TechnologyAccessResolver.project(team);
        assertFalse(locked.recipe(RECIPE).allowed());
        assertSame(locked, TechnologyAccessResolver.project(team));

        // The direct data API commits without a sync mark; the state revision still retires the entry.
        team.getData(FRSpecialDataTypes.KNOWLEDGE_DATA).acquireDesign(DESIGN);
        TechnologyAccessProjection unlocked = TechnologyAccessResolver.project(team);
        assertTrue(unlocked.recipe(RECIPE).allowed());
        assertSame(unlocked, TechnologyAccessResolver.project(team));
    }

    @Test
    void catalogueAndDefinitionInstallsRebuildEveryProjection() {
        TeamDataHolder team = team();
        TechnologyAccessProjection first = TechnologyAccessResolver.project(team);
        TechnologyAccessProjection ownerless = TechnologyAccessResolver.projection(null);
        assertSame(ownerless, TechnologyAccessResolver.projection(null));

        installCatalog();
        TechnologyAccessProjection afterCatalogue = TechnologyAccessResolver.project(team);
        assertNotSame(first, afterCatalogue);
        assertNotSame(ownerless, TechnologyAccessResolver.projection(null));

        KnowledgeDefinitions.install(KnowledgeDefinitions.Snapshot.empty());
        assertNotSame(afterCatalogue, TechnologyAccessResolver.project(team));
    }

    @Test
    void markedTeamReadsStayExactUntilTheFlush() {
        TeamDataHolder team = team();
        TechnologyAccessProjection settled = TechnologyAccessResolver.project(team);

        TeamResearchService.sync(team);
        TeamResearchService.sync(team);
        assertTrue(TeamResearchService.pending(team.getId()));
        TechnologyAccessProjection during = TechnologyAccessResolver.project(team);
        assertNotSame(settled, during);
        assertNotSame(during, TechnologyAccessResolver.project(team));

        // No member is online, so the flush settles both marks without building a snapshot.
        TeamResearchService.flush(team);
        assertFalse(TeamResearchService.pending(team.getId()));
        TechnologyAccessProjection after = TechnologyAccessResolver.project(team);
        assertSame(after, TechnologyAccessResolver.project(team));
    }

    @Test
    void reloadInvalidationDropsTeamAndOwnerlessProjections() {
        TeamDataHolder team = team();
        TechnologyAccessProjection teamProjection = TechnologyAccessResolver.project(team);
        TechnologyAccessProjection ownerless = TechnologyAccessResolver.projection(null);

        TechnologyAccessResolver.invalidateAll();
        assertNotSame(teamProjection, TechnologyAccessResolver.project(team));
        assertNotSame(ownerless, TechnologyAccessResolver.projection(null));
    }

    private static TeamDataHolder team() {
        return new TeamDataHolder(UUID.randomUUID(), new ClientTeam());
    }

    private static void installCatalog() {
        ResearchResultCatalog.install(new ResearchResultCatalog.Candidate(Map.of(id("topic"),
                new ResearchTopicDefinition(3, ResearchTopicDefinition.Presentation.EMPTY,
                        List.of(new ResearchResult.Design(DESIGN, List.of(RECIPE))), List.of())), Map.of()));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
