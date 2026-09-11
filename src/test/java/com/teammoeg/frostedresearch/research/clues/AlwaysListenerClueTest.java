package com.teammoeg.frostedresearch.research.clues;

import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AlwaysListenerClueTest {
    @BeforeEach
    @AfterEach
    void clearGlobalListeners() {
        ResearchHooks.getKillClues().clear();
        ResearchHooks.getTickClues().clear();
    }

    @Test
    void alwaysKillListenerUsesGlobalNullTeamScope() {
        Research research = research("always-kill-test");
        KillClue clue = new KillClue(
                "kill", "", "", "", 0.5F, false, true, null);

        assertDoesNotThrow(() -> clue.init(research));
        assertEquals(1, ResearchHooks.getKillClues().size());

        AtomicInteger calls = new AtomicInteger();
        Consumer<ClueClosure<KillClue>> count = ignored -> calls.incrementAndGet();
        ResearchHooks.getKillClues().call(UUID.randomUUID(), count);
        ResearchHooks.getKillClues().call(UUID.randomUUID(), count);
        assertEquals(2, calls.get(), "global listeners must apply to every team id");

        assertDoesNotThrow(() -> clue.removeListener(null, research));
        assertEquals(0, ResearchHooks.getKillClues().size());
    }

    @Test
    void alwaysTickListenerUsesGlobalNullTeamScope() {
        Research research = research("always-tick-test");
        TickListenerClue clue = new TickListenerClue(
                "tick", "", "", "", 0.5F, false, true) {
            @Override
            public boolean isCompleted(TeamResearchData data, ServerPlayer player) {
                return false;
            }

            @Override
            public String getBrief() {
                return "test";
            }
        };

        assertDoesNotThrow(() -> clue.init(research));
        assertEquals(1, ResearchHooks.getTickClues().size());

        AtomicInteger calls = new AtomicInteger();
        Consumer<ClueClosure<TickListenerClue>> count = ignored -> calls.incrementAndGet();
        ResearchHooks.getTickClues().call(UUID.randomUUID(), count);
        ResearchHooks.getTickClues().call(UUID.randomUUID(), count);
        assertEquals(2, calls.get(), "global listeners must apply to every team id");

        assertDoesNotThrow(() -> clue.removeListener(null, research));
        assertEquals(0, ResearchHooks.getTickClues().size());
    }

    private static Research research(String id) {
        Research research = new Research();
        research.setId(id);
        research.setCategory(ResearchCategory.RESCUE);
        return research;
    }
}
