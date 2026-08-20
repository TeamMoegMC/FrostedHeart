/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.frostedresearch.gui.archive.clue.ClueDestination;
import com.teammoeg.frostedresearch.gui.archive.clue.ResearchClueView;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchProjectWorkspaceContentTest {
    @Test
    void detailIncludesTheoryWhileDedicatedTheoryTabAndExperimentBoundaryRemain() {
        ResearchClueView detail = view("detail", ResearchWorkspaceState.ProjectTab.DETAIL, false);
        ResearchClueView theory = view("theory", ResearchWorkspaceState.ProjectTab.THEORY, false);
        ResearchClueView systemPoints = view("points", ResearchWorkspaceState.ProjectTab.DETAIL, true);
        List<ResearchClueView> views = List.of(detail, theory, systemPoints);

        assertEquals(List.of(detail, theory), ResearchProjectWorkspace.viewsForTab(
                views, ResearchWorkspaceState.ProjectTab.DETAIL));
        assertEquals(List.of(theory), ResearchProjectWorkspace.viewsForTab(
                views, ResearchWorkspaceState.ProjectTab.THEORY));
        assertTrue(ResearchProjectWorkspace.viewsForTab(
                views, ResearchWorkspaceState.ProjectTab.EXPERIMENT).isEmpty());
    }

    private static ResearchClueView view(
            String nonce, ResearchWorkspaceState.ProjectTab tab, boolean systemClue) {
        return new ResearchClueView(
                nonce,
                Component.literal(nonce),
                null,
                null,
                true,
                false,
                0.0F,
                ClueDestination.DETAILS,
                tab,
                0,
                systemClue);
    }
}
