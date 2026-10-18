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
package com.teammoeg.frostedresearch.gui.archive;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.teammoeg.frostedresearch.gui.archive.clue.ClueDestination;
import com.teammoeg.frostedresearch.gui.archive.clue.ResearchClueView;

import net.minecraft.network.chat.Component;

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
