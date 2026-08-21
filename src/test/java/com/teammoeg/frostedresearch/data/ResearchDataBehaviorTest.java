/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.data;

import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import com.teammoeg.frostedresearch.research.clues.CustomClue;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchDataBehaviorTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void totalCommittedCombinesDirectPointsAndTriggeredClueContribution() {
        Research research = researchWithClues();
        ResearchData data = new ResearchData(200, new boolean[]{true, false}, 0, Map.of(), Map.of());
        CustomClue optional = (CustomClue) research.getClues().get(1);

        data.setClueTriggered(optional, true);

        assertEquals(700L, data.getTotalCommitted(research));
        assertEquals(0.7F, data.getProgress(research), 0.0001F);
    }

    @Test
    void onlyRequiredCluesBlockCanComplete() {
        Research research = researchWithClues();
        ResearchData data = new ResearchData();
        CustomClue required = (CustomClue) research.getClues().get(0);
        CustomClue optional = (CustomClue) research.getClues().get(1);

        data.setClueTriggered(optional, true);
        assertFalse(data.canComplete(research));

        data.setClueTriggered(required, true);
        assertTrue(data.canComplete(research));
    }

    private static Research researchWithClues() {
        Research research = new Research();
        research.setId("behavior-test");
        research.setCategory(ResearchCategory.RESCUE);
        research.attachClue(new CustomClue("required", "Required", "Required desc", "Hint", 0.25F, true));
        research.attachClue(new CustomClue("optional", "Optional", "Optional desc", "Hint", 0.50F, false));
        return research;
    }
}
