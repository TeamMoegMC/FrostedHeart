/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownSimBedAssignmentTest {

    @Test
    void uuidOrderDeterminesUniqueSlotsRegardlessOfStorageOrder() {
        UUID first = new UUID(-5, 9);
        UUID second = new UUID(2, -7);
        UUID third = new UUID(2, 8);

        Map<UUID, Integer> forward = assign(new UUID[] { first, second, third }, new int[] { 90, 4, 71 });
        Map<UUID, Integer> reversed = assign(new UUID[] { third, first, second }, new int[] { 1, 500, 3 });

        assertEquals(forward, reversed);
		assertEquals(Map.of(first, 0, second, 1, third, 2), forward);
    }

    private static Map<UUID, Integer> assign(UUID[] uuids, int[] ids) {
        CitizenSim sim = new CitizenSim(uuids.length);
        IntArrayList indices = new IntArrayList(uuids.length);
        for (int k = 0; k < uuids.length; k++) {
            int index = sim.add(ids[k], 0, 0, 0, (byte) 0);
            sim.uuidHi[index] = uuids[k].getMostSignificantBits();
            sim.uuidLo[index] = uuids[k].getLeastSignificantBits();
            indices.add(index);
        }
        TownSimData.assignHomeSlots(sim, indices);
        Map<UUID, Integer> result = new HashMap<>();
        for (int i = 0; i < sim.size(); i++)
            result.put(new UUID(sim.uuidHi[i], sim.uuidLo[i]), sim.homeSlot[i]);
        return result;
    }
}
