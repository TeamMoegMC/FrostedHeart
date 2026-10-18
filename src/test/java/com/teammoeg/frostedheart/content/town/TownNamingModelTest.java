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

package com.teammoeg.frostedheart.content.town;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
