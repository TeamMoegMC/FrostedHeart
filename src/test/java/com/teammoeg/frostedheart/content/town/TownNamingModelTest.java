/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TownNamingModelTest {
    @Test
    void townNameMustContainVisibleTextAndIsBounded() {
        assertTrue(TownNamingModel.normalizeTownName("   ").isEmpty());
        assertEquals("New Hope", TownNamingModel.normalizeTownName("  New Hope  ").orElseThrow());
        assertEquals("AB", TownNamingModel.normalizeTownName("A\u00a7cB\n").orElseThrow());
        assertEquals(TownNamingModel.MAX_TOWN_NAME_LENGTH,
                TownNamingModel.normalizeTownName("x".repeat(100)).orElseThrow().length());
    }

    @Test
    void residentLastNameMayBeEmptyButFirstNameMayNot() {
        assertTrue(TownNamingModel.normalizeResidentName("", "Smith").isEmpty());
        TownNamingModel.ResidentName name = TownNamingModel.normalizeResidentName(" Ada ", " ")
                .orElseThrow();
        assertEquals("Ada", name.firstName());
        assertEquals("", name.lastName());
    }

    @Test
    void doesNotSplitUnicodeCodePointsAtLengthLimit() {
        String prefix = "a".repeat(TownNamingModel.MAX_RESIDENT_NAME_PART_LENGTH - 1);
        String normalized = TownNamingModel.normalizeResidentName(prefix + "😀", "")
                .orElseThrow().firstName();

        assertEquals(prefix, normalized);
    }
}
