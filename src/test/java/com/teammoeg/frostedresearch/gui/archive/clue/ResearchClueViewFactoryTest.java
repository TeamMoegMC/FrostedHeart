/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.archive.clue;

import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.gui.archive.ResearchOpenContext;
import com.teammoeg.frostedresearch.gui.archive.ResearchWorkspaceState;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import com.teammoeg.frostedresearch.research.clues.CustomClue;
import com.teammoeg.frostedresearch.research.clues.MinigameClue;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchClueViewFactoryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void viewSortingDoesNotMutateDefinitionOrderAndAddsReadOnlyPointsCondition() {
        Research research = new Research();
        research.setId("view-test");
        research.setCategory(ResearchCategory.PRODUCTION);
        CustomClue completedOptional =
                new CustomClue("optional", "Optional", "Optional desc", "Hint", 0.10F, false);
        MinigameClue requiredTheory =
                new MinigameClue("theory", "Theory", "Theory desc", "Hint", 0.25F, true, 1);
        research.attachClue(completedOptional);
        research.attachClue(requiredTheory);
        ResearchData data = new ResearchData(100, new boolean[]{true, false}, 0, Map.of(), Map.of());
        data.setClueTriggered(completedOptional, true);

        List<ResearchClueView> views = ResearchClueViewFactory.create(
                research,
                data,
                new ResearchClueViewFactory.Context(
                        ResearchOpenContext.Mode.DRAWING_DESK,
                        research.getId(),
                        requiredTheory.getNonce()));

        assertEquals(List.of(completedOptional, requiredTheory), research.getClues());
        assertEquals("theory", views.get(0).nonce());
        assertEquals(ResearchClueViewFactory.EXPERIMENT_POINTS_NONCE, views.get(1).nonce());
        assertEquals("optional", views.get(2).nonce());
        assertEquals(ClueDestination.THEORY_GAME, views.get(0).destination());
        assertEquals(ResearchWorkspaceState.ProjectTab.THEORY, views.get(0).tab());
        assertEquals(ResearchWorkspaceState.ProjectTab.DETAIL, views.get(1).tab());
        assertEquals(ResearchWorkspaceState.ProjectTab.DETAIL, views.get(2).tab());
        assertTrue(views.get(1).systemClue());
        assertFalse(views.get(1).completed());
    }

    @Test
    void browseModeNeverOffersDrawingDeskNavigation() {
        MinigameClue clue = new MinigameClue("theory", "Theory", "Desc", "Hint", 0.25F, true, 1);

        ClueDestination destination = ClueDestinationResolver.resolve(
                clue,
                false,
                new ClueDestinationResolver.Context(ResearchOpenContext.Mode.BROWSE, true, true));

        assertEquals(ClueDestination.DRAWING_DESK_REQUIRED, destination);
    }
}
