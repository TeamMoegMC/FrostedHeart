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

class ResearchTypeListPanelCacheTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void visibleProjectsAreCachedUntilARelevantInputChanges() {
        ResearchWorkspaceState state = new ResearchWorkspaceState(ResearchOpenContext.browse());
        ResearchTypeListPanel panel = new ResearchTypeListPanel(null, state, () -> { });
        Research alpha = research("alpha", ResearchCategory.PRODUCTION);
        Research beta = research("beta", ResearchCategory.PRODUCTION);

        panel.setDefinitions(List.of(alpha, beta));
        assertEquals(1, panel.visibleResearchBuildCountForTest());
        assertEquals(List.of(alpha, beta), panel.visibleResearchesForTest());
        assertEquals(List.of(alpha, beta), panel.visibleResearchesForTest());
        assertEquals(1, panel.visibleResearchBuildCountForTest());

        state.setSearchQuery("alpha");
        panel.onFilterChanged();
        assertEquals(List.of(alpha), panel.visibleResearchesForTest());
        assertEquals(2, panel.visibleResearchBuildCountForTest());

        state.setBookmarked(beta.getId(), true);
        panel.onBookmarksChanged();
        assertEquals(3, panel.visibleResearchBuildCountForTest());

        panel.onProgressChanged(alpha.getId());
        assertEquals(3, panel.visibleResearchBuildCountForTest());

        panel.setDefinitions(List.of(alpha));
        assertEquals(4, panel.visibleResearchBuildCountForTest());
    }

    private static Research research(String id, ResearchCategory category) {
        Research research = new Research();
        research.setId(id);
        research.setCategory(category);
        return research;
    }
}
