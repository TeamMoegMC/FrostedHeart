/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyOffsetMaskTest {
    @Test
    void predefinedMasksAreBoundedAndAlwaysContainSelf() {
        assertEquals(1, DependencyOffsetMask.SELF_ONLY.offsetCount());
        assertEquals(7, DependencyOffsetMask.NEIGHBOR_6.offsetCount());
        assertEquals(27, DependencyOffsetMask.NEIGHBOR_26.offsetCount());

        for (DependencyOffsetMask mask : Set.of(
                DependencyOffsetMask.SELF_ONLY,
                DependencyOffsetMask.NEIGHBOR_6,
                DependencyOffsetMask.NEIGHBOR_26)) {
            assertTrue(mask.contains(DependencyOffsetMask.SELF));
            assertTrue(mask.offsets().stream().allMatch(offset ->
                    Math.abs(offset.x()) <= 1
                            && Math.abs(offset.y()) <= 1
                            && Math.abs(offset.z()) <= 1));
        }
        assertFalse(DependencyOffsetMask.NEIGHBOR_6.contains(1, 1, 0));
        assertTrue(DependencyOffsetMask.NEIGHBOR_26.contains(1, 1, 0));
    }

    @Test
    void explicitMaskAddsSelfAndHasStableRoundTrip() {
        DependencyOffsetMask mask = DependencyOffsetMask.explicit(
                new DependencyOffsetMask.Offset(1, 0, 0),
                new DependencyOffsetMask.Offset(0, -1, 1)
        );

        assertEquals(3, mask.offsetCount());
        assertTrue(mask.contains(DependencyOffsetMask.SELF));
        assertEquals(mask, DependencyOffsetMask.fromEncodedBits(mask.encodedBits()));
        assertEquals(DependencyOffsetMask.explicit(
                        new DependencyOffsetMask.Offset(-1, 0, 0),
                        new DependencyOffsetMask.Offset(0, 1, -1)),
                mask.reversed());

        int selfBit = DependencyOffsetMask.SELF_ONLY.encodedBits();
        assertThrows(IllegalArgumentException.class,
                () -> DependencyOffsetMask.fromEncodedBits(selfBit | (1 << 30)));
        assertThrows(IllegalArgumentException.class,
                () -> DependencyOffsetMask.fromEncodedBits(1));
        assertThrows(IllegalArgumentException.class,
                () -> new DependencyOffsetMask.Offset(2, 0, 0));
    }

    @Test
    void neighbor26MutationAndReadClosuresMatchFrozenBudgets() {
        Set<Point> affectedCenters = new HashSet<>();
        for (DependencyOffsetMask.Offset dependency : DependencyOffsetMask.NEIGHBOR_26.offsets()) {
            affectedCenters.add(new Point(-dependency.x(), -dependency.y(), -dependency.z()));
        }
        assertEquals(27, affectedCenters.size());

        Set<Point> mutationReadClosure = new HashSet<>();
        for (Point center : affectedCenters) {
            addReads(mutationReadClosure, center);
        }
        assertEquals(125, mutationReadClosure.size());
        assertEquals(Set.of(-2, -1, 0, 1, 2), axisValues(mutationReadClosure, Axis.X));
        assertEquals(Set.of(-2, -1, 0, 1, 2), axisValues(mutationReadClosure, Axis.Y));
        assertEquals(Set.of(-2, -1, 0, 1, 2), axisValues(mutationReadClosure, Axis.Z));

        Set<Point> coldBrickReadClosure = new HashSet<>();
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    addReads(coldBrickReadClosure, new Point(x, y, z));
                }
            }
        }
        assertEquals(216, coldBrickReadClosure.size());
        assertEquals(Set.of(-1, 0, 1, 2, 3, 4), axisValues(coldBrickReadClosure, Axis.X));
    }

    private static void addReads(Set<Point> closure, Point center) {
        for (DependencyOffsetMask.Offset read : DependencyOffsetMask.NEIGHBOR_26.offsets()) {
            closure.add(new Point(
                    center.x() + read.x(),
                    center.y() + read.y(),
                    center.z() + read.z()
            ));
        }
    }

    private static Set<Integer> axisValues(Set<Point> points, Axis axis) {
        Set<Integer> values = new HashSet<>();
        for (Point point : points) {
            values.add(switch (axis) {
                case X -> point.x();
                case Y -> point.y();
                case Z -> point.z();
            });
        }
        return values;
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    private record Point(int x, int y, int z) {
    }
}
