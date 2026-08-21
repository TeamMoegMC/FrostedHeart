/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.chorda.client.cui.base.PanZoomViewport.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchWorkspaceStateTest {
    @Test
    void camerasAndListScrollAreRememberedPerNormalizedType() {
        ResearchWorkspaceState state = new ResearchWorkspaceState(ResearchOpenContext.browse());

        assertFalse(state.hasCamera("frostedheart:production"));
        state.setCamera("frostedheart:production", new Camera(12.0D, 30.0D, 4.0D));
        state.setTypeListScroll("production", 9);

        assertTrue(state.hasCamera("frostedresearch:production"));
        assertFalse(state.hasCamera("frostedresearch:living"));
        assertEquals(new Camera(12.0D, 30.0D, ResearchWorkspaceState.MAX_ZOOM),
                state.camera("frostedresearch:production"));
        assertEquals(9, state.typeListScroll("frostedheart:production"));
        assertEquals(Camera.DEFAULT, state.camera("frostedresearch:living"));
    }

    @Test
    void overviewZoomClampsAtFifteenPercentWithoutDroppingContent() {
        ResearchWorkspaceState state = new ResearchWorkspaceState(ResearchOpenContext.browse());

        state.setCamera("*", new Camera(0.0D, 0.0D, 0.001D));

        assertEquals(0.15D, ResearchWorkspaceState.MIN_ZOOM);
        assertEquals(ResearchWorkspaceState.MIN_ZOOM, state.camera("*").zoom());
        assertEquals(22, ResearchGraphViewport.scaledNodeLength(144.0D, ResearchWorkspaceState.MIN_ZOOM));
        assertEquals(8, ResearchGraphViewport.scaledNodeLength(52.0D, ResearchWorkspaceState.MIN_ZOOM));
        assertEquals(4, ResearchGraphViewport.scaledNodeIconLength(ResearchWorkspaceState.MIN_ZOOM));
        assertEquals(0.25F, ResearchGraphViewport.nodeTextScale(ResearchWorkspaceState.MIN_ZOOM));
        assertEquals(0.55F, ResearchGraphViewport.nodeTextScale(0.55D));
    }

    @Test
    void researchFieldsShareTheTopHeaderWithTheDrawingDeskAction() {
        assertTrue(ResearchArchiveLayer.FIELD_TABS_Y < ResearchArchiveLayer.HEADER_HEIGHT);
        assertEquals(ResearchArchiveLayer.HEADER_HEIGHT + 4, ResearchArchiveLayer.CONTENT_TOP);
        assertTrue(ResearchArchiveLayer.HEADER_TITLE_WIDTH
                + ResearchArchiveLayer.HEADER_ACTION_WIDTH < 280);
    }

    @Test
    void definitionReloadRetainsValidViewStateAndFallsBackSelection() {
        ResearchWorkspaceState state = new ResearchWorkspaceState(new ResearchOpenContext(
                ResearchOpenContext.Mode.DRAWING_DESK,
                "removed",
                ResearchWorkspaceState.ProjectTab.THEORY,
                "removed-clue"));
        state.setResearchTypeFilter("removed:type");
        state.setBookmarked("removed", true);

        state.retainDefinitions(
                List.of("current", "first"),
                List.of("frostedresearch:production"),
                "current",
                "first");

        assertEquals("current", state.selectedResearchId());
        assertNull(state.selectedClueNonce());
        assertEquals("*", state.researchTypeFilter());
        assertTrue(state.bookmarkedResearchIds().isEmpty());
        assertTrue(state.projectWorkspaceOpen());
    }

    @Test
    void drawingDeskBackOrderClosesProjectThenReturnsToDesk() {
        ResearchOpenContext context = ResearchOpenContext.drawingDesk("research");
        ResearchWorkspaceState state = new ResearchWorkspaceState(context);
        AtomicBoolean closed = new AtomicBoolean();
        StatefulResearchNavigationController navigation =
                new StatefulResearchNavigationController(context, state, () -> closed.set(true));

        state.setSurface(ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE);
        assertTrue(navigation.back());
        assertFalse(state.projectWorkspaceOpen());
        assertTrue(navigation.back());
        assertEquals(ResearchWorkspaceState.Surface.DRAWING_DESK, state.surface());
        assertFalse(navigation.back());
        assertFalse(closed.get());

        navigation.returnToWorld();
        assertTrue(closed.get());
    }

    @Test
    void browseContextCannotRouteIntoDrawingDesk() {
        ResearchOpenContext context = ResearchOpenContext.browse();
        ResearchWorkspaceState state = new ResearchWorkspaceState(context);
        StatefulResearchNavigationController navigation =
                new StatefulResearchNavigationController(context, state, () -> { });

        navigation.goToDrawingDesk(ResearchWorkspaceState.DrawDeskFocusTarget.ITEM_EXAMINE);

        assertEquals(ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE, state.surface());
        assertEquals(ResearchWorkspaceState.DrawDeskFocusTarget.NONE, state.consumeDrawDeskFocusTarget());
    }

    @Test
    void invalidCameraAndEmptySelectionCannotLeaveBrokenUiState() {
        ResearchWorkspaceState state = new ResearchWorkspaceState(new ResearchOpenContext(
                ResearchOpenContext.Mode.BROWSE,
                "selected",
                ResearchWorkspaceState.ProjectTab.DETAIL,
                "clue"));

        state.setCamera("production", new Camera(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY));
        state.selectResearch(null);

        assertEquals(Camera.DEFAULT, state.camera("production"));
        assertFalse(state.projectWorkspaceOpen());
        assertNull(state.selectedClueNonce());
    }
}
