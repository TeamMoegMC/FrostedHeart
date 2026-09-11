/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.frostedresearch.data.TeamResearchData;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ResearchDataAPITest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
