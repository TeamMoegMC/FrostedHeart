/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResearchGraphViewportPerformanceTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void searchRefreshDoesNotRebuildTheCategoryProjection() {
        ResearchWorkspaceState state = new ResearchWorkspaceState(ResearchOpenContext.browse());
        ResearchGraphViewport viewport = new ResearchGraphViewport(null, state, navigation(), () -> { });
        viewport.setDefinitions(List.of(research("alpha"), research("beta")), 1L);

        assertEquals(1, viewport.projectionBuildCountForTest());
        state.setSearchQuery("alpha");
        viewport.onSearchChanged();
        viewport.onSearchChanged();
        assertEquals(1, viewport.projectionBuildCountForTest());

        state.setResearchTypeFilter("frostedresearch:production");
        viewport.onResearchTypeChanged();
        assertEquals(2, viewport.projectionBuildCountForTest());
    }

    private static Research research(String id) {
        Research research = new Research();
        research.setId(id);
        research.setCategory(ResearchCategory.PRODUCTION);
        return research;
    }

    private static ResearchNavigationController navigation() {
        return new ResearchNavigationController() {
            @Override
            public void openResearch(String researchId) {
            }

            @Override
            public void openClue(String researchId, String clueNonce) {
            }

            @Override
            public void goToDrawingDesk(ResearchWorkspaceState.DrawDeskFocusTarget target) {
            }

            @Override
            public void returnToWorld() {
            }

            @Override
            public boolean back() {
                return false;
            }
        };
    }
}
