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
