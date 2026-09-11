/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch;

import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FHResearchRenderOrderTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void legacyRenderOrderGroupsProjectsAndRetainsItsFrontInsertionSemantics() {
        RenderResearch lockedFirst = research("locked-first", false, false, false, false, false);
        RenderResearch rewardFirst = research("reward-first", false, true, true, true, false);
        RenderResearch availableFirst = research("available-first", false, false, true, true, false);
        RenderResearch showableFirst = research("showable-first", false, false, false, true, false);
        RenderResearch completedFirst = research("completed-first", false, true, true, true, false);
        RenderResearch rewardSecond = research("reward-second", false, true, true, true, false);
        RenderResearch availableSecond = research("available-second", false, false, true, true, false);
        RenderResearch showableSecond = research("showable-second", false, false, false, true, false);
        RenderResearch completedSecond = research("completed-second", false, true, true, true, false);
        RenderResearch hidden = research("hidden", true, true, true, true, false);

        rewardFirst.unclaimedReward = true;
        rewardSecond.unclaimedReward = true;
        List<Research> definitions = List.of(
                lockedFirst, rewardFirst, availableFirst, showableFirst, completedFirst,
                rewardSecond, availableSecond, showableSecond, completedSecond, hidden);

        assertIds(
                List.of("reward-second", "reward-first", "available-first", "available-second",
                        "showable-first", "showable-second", "completed-first", "completed-second"),
                FHResearch.getResearchesForRender(definitions, false));
        assertIds(
                List.of("reward-second", "reward-first", "available-first", "available-second",
                        "showable-first", "showable-second", "completed-first", "completed-second",
                        "locked-first", "hidden"),
                FHResearch.getResearchesForRender(definitions, true));
    }

    private static RenderResearch research(
            String id, boolean hidden, boolean completed, boolean unlocked, boolean showable, boolean unclaimedReward) {
        RenderResearch research = new RenderResearch();
        research.setId(id);
        research.hidden = hidden;
        research.completed = completed;
        research.unlocked = unlocked;
        research.showable = showable;
        research.unclaimedReward = unclaimedReward;
        return research;
    }

    private static void assertIds(List<String> expected, List<Research> actual) {
        assertEquals(expected, actual.stream().map(Research::getId).toList());
    }

    private static final class RenderResearch extends Research {
        private boolean hidden;
        private boolean completed;
        private boolean unlocked;
        private boolean showable;
        private boolean unclaimedReward;

        @Override
        public boolean isHidden() {
            return hidden;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public boolean isUnlocked() {
            return unlocked;
        }

        @Override
        public boolean isShowable() {
            return showable;
        }

        @Override
        public boolean hasUnclaimedReward() {
            return unclaimedReward;
        }
    }
}
