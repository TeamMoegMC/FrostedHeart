package com.teammoeg.frostedresearch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchHooksTest {
    @Test
    void incompleteMatchingKillClueCompletes() {
        assertTrue(ResearchHooks.shouldCompleteKillClue(false, true));
    }

    @Test
    void completedOrMismatchedKillClueDoesNotCompleteAgain() {
        assertFalse(ResearchHooks.shouldCompleteKillClue(true, true));
        assertFalse(ResearchHooks.shouldCompleteKillClue(false, false));
        assertFalse(ResearchHooks.shouldCompleteKillClue(true, false));
    }
}
