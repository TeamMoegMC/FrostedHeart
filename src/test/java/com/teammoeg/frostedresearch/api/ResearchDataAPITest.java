/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import net.minecraft.nbt.LongTag;

class ResearchDataAPITest {
    @BeforeAll
    static void bootstrapMinecraft() {
        CUtils.startTestEnvironment();
    }

    @Test
    void stringLongVariantUsesLongTagWithoutPrecisionLoss() {
        TeamResearchData data = new TeamResearchData();
        long value = 9_007_199_254_740_993L;

        ResearchDataAPI.putVariantLong(data, "large", value);

        assertInstanceOf(LongTag.class, data.getVariants().get("large"));
        assertEquals(value, data.getVariants().getLong("large"));
    }
}
