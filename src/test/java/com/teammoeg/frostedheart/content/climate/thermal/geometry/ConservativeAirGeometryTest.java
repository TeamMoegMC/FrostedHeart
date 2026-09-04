/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConservativeAirGeometryTest {
    @Test
    void emptyBlockIsOneRegionWithEveryFaceOpen() {
        ConservativeAirGeometry.Resolution resolution =
                ConservativeAirGeometry.resolve(List.of(), 8);

        assertEquals(ConservativeAirGeometry.Status.RESOLVED, resolution.status());
        assertEquals(1, resolution.components().size());
        ConservativeAirGeometry.AirComponent component = resolution.components().get(0);
        assertEquals(64, Long.bitCount(component.microcellMask()));
        for (ConservativeAirGeometry.Face face : ConservativeAirGeometry.Face.values()) {
            assertEquals(ConservativeAirGeometry.FULL_FACE_MASK, component.faceMask(face));
        }
    }

    @Test
    void fullBlockHasNoAirRegionOrAperture() {
        ConservativeAirGeometry.Resolution resolution = ConservativeAirGeometry.resolve(
                List.of(ConservativeAirGeometry.UnitBox.fullBlock()), 8);

        assertEquals(ConservativeAirGeometry.Status.RESOLVED, resolution.status());
        assertTrue(resolution.components().isEmpty());
        assertEquals(-1, componentAt(resolution, 0, 0, 0));
        assertEquals(0, combinedFaceMask(
                resolution, ConservativeAirGeometry.Face.POSITIVE_X));
    }

    @Test
    void thinInternalSlabConservativelySeparatesTwoRegions() {
        ConservativeAirGeometry.UnitBox slab = new ConservativeAirGeometry.UnitBox(
                0.49D, 0.0D, 0.0D, 0.51D, 1.0D, 1.0D);
        ConservativeAirGeometry.Resolution resolution =
                ConservativeAirGeometry.resolve(List.of(slab), 8);

        assertEquals(2, resolution.components().size());
        assertEquals(0, componentAt(resolution, 0, 1, 1));
        assertEquals(-1, componentAt(resolution, 1, 1, 1));
        assertEquals(-1, componentAt(resolution, 2, 1, 1));
        assertEquals(1, componentAt(resolution, 3, 1, 1));
        assertEquals(16, Long.bitCount(
                resolution.components().get(0).microcellMask()));
        assertEquals(16, Long.bitCount(
                resolution.components().get(1).microcellMask()));
        assertEquals(ConservativeAirGeometry.FULL_FACE_MASK,
                resolution.components().get(0).negativeXMask());
        assertEquals(0, resolution.components().get(0).positiveXMask());
        assertEquals(0, resolution.components().get(1).negativeXMask());
        assertEquals(ConservativeAirGeometry.FULL_FACE_MASK,
                resolution.components().get(1).positiveXMask());
    }

    @Test
    void partialObstructionClosesTheWholeQuarterFaceTile() {
        ConservativeAirGeometry.UnitBox tinyCornerObstruction = new ConservativeAirGeometry.UnitBox(
                0.0D, 0.01D, 0.01D, 0.01D, 0.02D, 0.02D);
        ConservativeAirGeometry.Resolution resolution =
                ConservativeAirGeometry.resolve(List.of(tinyCornerObstruction), 8);

        assertEquals(-1, componentAt(resolution, 0, 0, 0));
        assertEquals(ConservativeAirGeometry.FULL_FACE_MASK ^ 1,
                combinedFaceMask(
                        resolution,
                        ConservativeAirGeometry.Face.NEGATIVE_X));
        assertEquals(63, Long.bitCount(resolution.provenAirMicrocellMask()));
    }

    @Test
    void faceBitLayoutIsStableAndWorldAxisExplicit() {
        List<ConservativeAirGeometry.UnitBox> blockers = new ArrayList<>();
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    if (x == 0 && y == 2 && z == 3) {
                        continue;
                    }
                    blockers.add(microcell(x, y, z));
                }
            }
        }

        ConservativeAirGeometry.AirComponent component =
                ConservativeAirGeometry.resolve(blockers, 1).components().get(0);

        assertEquals(1 << 11, component.negativeXMask());
        assertEquals(0, component.positiveXMask());
        assertEquals(0, component.negativeYMask());
        assertEquals(0, component.positiveYMask());
        assertEquals(0, component.negativeZMask());
        assertEquals(1 << 8, component.positiveZMask());
    }

    @Test
    void regionLimitProducesObservableUnsupportedInsteadOfNarrowing() {
        ConservativeAirGeometry.UnitBox slab = new ConservativeAirGeometry.UnitBox(
                0.49D, 0.0D, 0.0D, 0.51D, 1.0D, 1.0D);
        ConservativeAirGeometry.Resolution resolution =
                ConservativeAirGeometry.resolve(List.of(slab), 1);

        assertEquals(ConservativeAirGeometry.Status.CONSERVATIVE_UNSUPPORTED, resolution.status());
        assertTrue(resolution.components().isEmpty());
    }

    @Test
    void invalidInputsAreRejectedBeforeCompilation() {
        assertThrows(IllegalArgumentException.class, () -> new ConservativeAirGeometry.UnitBox(
                0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new ConservativeAirGeometry.UnitBox(
                -0.1D, 0.0D, 0.0D, 0.1D, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () ->
                ConservativeAirGeometry.resolve(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () ->
                ConservativeAirGeometry.resolve(java.util.Arrays.asList((ConservativeAirGeometry.UnitBox) null), 1));
    }

    @Test
    void randomizedBoundaryProjectionNeverCreatesFalseOpening() {
        Random random = new Random(0x46524f535445444cL);
        for (int fixture = 0; fixture < 200; fixture++) {
            List<ConservativeAirGeometry.UnitBox> blockers = new ArrayList<>();
            int blockerCount = 1 + random.nextInt(6);
            for (int blocker = 0; blocker < blockerCount; blocker++) {
                blockers.add(randomBoundaryBiasedBox(random));
            }
            ConservativeAirGeometry.Resolution resolution =
                    ConservativeAirGeometry.resolve(blockers, 64);

            assertEquals(ConservativeAirGeometry.Status.RESOLVED, resolution.status());
            for (ConservativeAirGeometry.Face face : ConservativeAirGeometry.Face.values()) {
                int aperture = combinedFaceMask(resolution, face);
                for (int bit = 0; bit < 16; bit++) {
                    if ((aperture & (1 << bit)) != 0) {
                        assertFalseBoundaryIntersection(blockers, face, bit);
                    }
                }
            }
        }
    }

    @Test
    void xMirrorPreservesRegionCountAndSwapsExteriorMasks() {
        List<ConservativeAirGeometry.UnitBox> blockers = List.of(
                new ConservativeAirGeometry.UnitBox(0.0D, 0.1D, 0.2D, 0.3D, 0.6D, 0.9D),
                new ConservativeAirGeometry.UnitBox(0.45D, 0.0D, 0.0D, 0.55D, 0.4D, 1.0D)
        );
        List<ConservativeAirGeometry.UnitBox> mirrored = blockers.stream()
                .map(box -> new ConservativeAirGeometry.UnitBox(
                        1.0D - box.maxX(), box.minY(), box.minZ(),
                        1.0D - box.minX(), box.maxY(), box.maxZ()))
                .toList();

        ConservativeAirGeometry.Resolution original =
                ConservativeAirGeometry.resolve(blockers, 64);
        ConservativeAirGeometry.Resolution reflected =
                ConservativeAirGeometry.resolve(mirrored, 64);

        assertEquals(original.components().size(), reflected.components().size());
        assertEquals(Long.bitCount(original.provenAirMicrocellMask()),
                Long.bitCount(reflected.provenAirMicrocellMask()));
        assertEquals(combinedFaceMask(
                        original, ConservativeAirGeometry.Face.NEGATIVE_X),
                combinedFaceMask(
                        reflected, ConservativeAirGeometry.Face.POSITIVE_X));
        assertEquals(combinedFaceMask(
                        original, ConservativeAirGeometry.Face.POSITIVE_X),
                combinedFaceMask(
                        reflected, ConservativeAirGeometry.Face.NEGATIVE_X));
    }

    private static int componentAt(
            ConservativeAirGeometry.Resolution resolution,
            int x,
            int y,
            int z
    ) {
        for (ConservativeAirGeometry.AirComponent component
                : resolution.components()) {
            int microcell = (y << 4) | (z << 2) | x;
            if ((component.microcellMask() & 1L << microcell) != 0L) {
                return component.id();
            }
        }
        return -1;
    }

    private static int combinedFaceMask(
            ConservativeAirGeometry.Resolution resolution,
            ConservativeAirGeometry.Face face
    ) {
        int mask = 0;
        for (ConservativeAirGeometry.AirComponent component
                : resolution.components()) {
            mask |= component.faceMask(face);
        }
        return mask;
    }

    private static ConservativeAirGeometry.UnitBox microcell(int x, int y, int z) {
        double size = 0.25D;
        return new ConservativeAirGeometry.UnitBox(
                x * size,
                y * size,
                z * size,
                (x + 1) * size,
                (y + 1) * size,
                (z + 1) * size
        );
    }

    private static ConservativeAirGeometry.UnitBox randomBoundaryBiasedBox(Random random) {
        double minX = randomCoordinate(random, false);
        double minY = randomCoordinate(random, false);
        double minZ = randomCoordinate(random, false);
        double maxX = randomCoordinateAfter(random, minX);
        double maxY = randomCoordinateAfter(random, minY);
        double maxZ = randomCoordinateAfter(random, minZ);
        return new ConservativeAirGeometry.UnitBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static double randomCoordinate(Random random, boolean allowOne) {
        int maximum = allowOne ? 8 : 7;
        return random.nextInt(maximum + 1) / 8.0D;
    }

    private static double randomCoordinateAfter(Random random, double minimum) {
        int minimumStep = (int) Math.round(minimum * 8.0D) + 1;
        return (minimumStep + random.nextInt(9 - minimumStep)) / 8.0D;
    }

    private static void assertFalseBoundaryIntersection(
            List<ConservativeAirGeometry.UnitBox> blockers,
            ConservativeAirGeometry.Face face,
            int bit
    ) {
        int u = bit & 3;
        int v = (bit >>> 2) & 3;
        double minU = u / 4.0D;
        double maxU = (u + 1) / 4.0D;
        double minV = v / 4.0D;
        double maxV = (v + 1) / 4.0D;
        for (ConservativeAirGeometry.UnitBox box : blockers) {
            boolean intersects = switch (face) {
                case NEGATIVE_X -> box.minX() == 0.0D
                        && overlaps(box.minZ(), box.maxZ(), minU, maxU)
                        && overlaps(box.minY(), box.maxY(), minV, maxV);
                case POSITIVE_X -> box.maxX() == 1.0D
                        && overlaps(box.minZ(), box.maxZ(), minU, maxU)
                        && overlaps(box.minY(), box.maxY(), minV, maxV);
                case NEGATIVE_Y -> box.minY() == 0.0D
                        && overlaps(box.minX(), box.maxX(), minU, maxU)
                        && overlaps(box.minZ(), box.maxZ(), minV, maxV);
                case POSITIVE_Y -> box.maxY() == 1.0D
                        && overlaps(box.minX(), box.maxX(), minU, maxU)
                        && overlaps(box.minZ(), box.maxZ(), minV, maxV);
                case NEGATIVE_Z -> box.minZ() == 0.0D
                        && overlaps(box.minX(), box.maxX(), minU, maxU)
                        && overlaps(box.minY(), box.maxY(), minV, maxV);
                case POSITIVE_Z -> box.maxZ() == 1.0D
                        && overlaps(box.minX(), box.maxX(), minU, maxU)
                        && overlaps(box.minY(), box.maxY(), minV, maxV);
            };
            assertFalse(intersects, "open quarter-face tile intersects supplied blocker projection");
        }
    }

    private static boolean overlaps(double minA, double maxA, double minB, double maxB) {
        return maxA > minB && minA < maxB;
    }
}
