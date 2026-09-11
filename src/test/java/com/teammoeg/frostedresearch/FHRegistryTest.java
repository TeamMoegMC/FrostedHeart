/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
