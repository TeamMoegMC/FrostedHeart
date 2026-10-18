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
package com.teammoeg.frostedresearch;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class FHRegistryTest {
    @Test
    void runIfPresentUsesZeroBasedIndexesAndChecksBothBounds() {
        FHRegistry<Entry> registry = new FHRegistry<>();
        registry.register(new Entry("zero"));
        registry.register(new Entry("one"));
        List<String> visited = new ArrayList<>();

        registry.runIfPresent(-1, entry -> visited.add(entry.getId()));
        registry.runIfPresent(0, entry -> visited.add(entry.getId()));
        registry.runIfPresent(1, entry -> visited.add(entry.getId()));
        registry.runIfPresent(2, entry -> visited.add(entry.getId()));

        assertEquals(List.of("zero", "one"), visited);
    }

    private record Entry(String id) implements FHRegisteredItem {
        @Override
        public String getId() {
            return id;
        }
    }
}
