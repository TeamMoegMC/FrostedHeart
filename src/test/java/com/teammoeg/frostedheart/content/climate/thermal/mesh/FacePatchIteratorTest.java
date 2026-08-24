/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacePatchIteratorTest {
    @Test
    void sixteenToSixteenProducesOneExactPatchForEveryAxis() {
        for (FacePatchIterator.Axis axis : FacePatchIterator.Axis.values()) {
            Partition partition = partition(axis, 16, 16, 16, -16, -16, CoarseSide.NEGATIVE);

            FacePatchIterator iterator = FacePatchIterator.between(
                    axis, partition.negative(), partition.positive());

            assertPartition(iterator, partition, axis, 16, 1, 256L, 16.0D);
            FacePatchIterator.FacePatch patch = iterator.patches().get(0);
            assertEquals(16, patch.edgeLengthBlocks());
            assertEquals(partition.negative().get(0), patch.negativeSideCell());
            assertEquals(partition.positive().get(0), patch.positiveSideCell());
        }
    }

    @Test
    void sixteenToFourProducesSixteenPatchesWithCoarseOnEitherSide() {
        for (FacePatchIterator.Axis axis : FacePatchIterator.Axis.values()) {
            for (CoarseSide coarseSide : CoarseSide.values()) {
                Partition partition = partition(
                        axis, 16, 4, 16, -16, -16, coarseSide);

                FacePatchIterator iterator = FacePatchIterator.between(
                        axis, partition.negative(), partition.positive());

                assertPartition(iterator, partition, axis, 16, 16, 256L, 10.0D);
                for (FacePatchIterator.FacePatch patch : iterator) {
                    assertEquals(4, patch.edgeLengthBlocks());
                    assertEquals(16L, patch.overlapAreaBlocksSquared());
                }
            }
        }
    }

    @Test
    void eightToFourProducesFourPatchesWithCoarseOnEitherSide() {
        for (FacePatchIterator.Axis axis : FacePatchIterator.Axis.values()) {
            for (CoarseSide coarseSide : CoarseSide.values()) {
                Partition partition = partition(axis, 8, 4, 8, -8, -8, coarseSide);

                FacePatchIterator iterator = FacePatchIterator.between(
                        axis, partition.negative(), partition.positive());

                assertPartition(iterator, partition, axis, 8, 4, 64L, 6.0D);
                for (FacePatchIterator.FacePatch patch : iterator) {
                    assertEquals(4, patch.edgeLengthBlocks());
                    assertEquals(16L, patch.overlapAreaBlocksSquared());
                }
            }
        }
    }

    @Test
    void canonicalOrderDoesNotDependOnPartitionListOrder() {
        for (FacePatchIterator.Axis axis : FacePatchIterator.Axis.values()) {
            Partition partition = partition(
                    axis, 16, 4, -16, -16, -16, CoarseSide.POSITIVE);
            List<FacePatchIterator.Cell> reversedNegative =
                    new ArrayList<>(partition.negative());
            java.util.Collections.reverse(reversedNegative);

            List<CanonicalFacePatchKey> expected = FacePatchIterator.between(
                            axis, partition.negative(), partition.positive())
                    .patches().stream()
                    .map(FacePatchIterator.FacePatch::key)
                    .toList();
            List<CanonicalFacePatchKey> actual = FacePatchIterator.between(
                            axis, reversedNegative, partition.positive())
                    .patches().stream()
                    .map(FacePatchIterator.FacePatch::key)
                    .toList();

            assertEquals(expected, actual);
            assertEquals(-16, normalCoordinate(axis, actual.get(0)));
        }
    }

    @Test
    void canonicalKeyRoundTripsSignedCoordinatesAndSeparatesAxes() {
        Set<CanonicalFacePatchKey> axisKeys = new HashSet<>();
        for (FacePatchIterator.Axis axis : FacePatchIterator.Axis.values()) {
            CanonicalFacePatchKey key = CanonicalFacePatchKey.of(
                    axis,
                    CanonicalFacePatchKey.MIN_HORIZONTAL_COORDINATE,
                    CanonicalFacePatchKey.MIN_VERTICAL_COORDINATE,
                    CanonicalFacePatchKey.MAX_HORIZONTAL_COORDINATE
            );
            assertEquals(CanonicalFacePatchKey.MIN_HORIZONTAL_COORDINATE, key.worldX());
            assertEquals(CanonicalFacePatchKey.MIN_VERTICAL_COORDINATE, key.worldY());
            assertEquals(CanonicalFacePatchKey.MAX_HORIZONTAL_COORDINATE, key.worldZ());
            axisKeys.add(key);
        }
        assertEquals(3, axisKeys.size());

        assertThrows(IllegalArgumentException.class, () -> CanonicalFacePatchKey.of(
                FacePatchIterator.Axis.X,
                CanonicalFacePatchKey.MAX_HORIZONTAL_COORDINATE + 1,
                0,
                0));
        assertThrows(IllegalArgumentException.class, () -> CanonicalFacePatchKey.of(
                FacePatchIterator.Axis.X,
                0,
                CanonicalFacePatchKey.MIN_VERTICAL_COORDINATE - 1,
                0));
    }

    @Test
    void cellsRejectUnsupportedWidthsMisalignmentAndUnpackableBounds() {
        assertThrows(IllegalArgumentException.class, () ->
                new FacePatchIterator.Cell(0, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () ->
                new FacePatchIterator.Cell(4, 0, 0, 8));
        assertThrows(IllegalArgumentException.class, () ->
                new FacePatchIterator.Cell(
                        CanonicalFacePatchKey.MAX_HORIZONTAL_COORDINATE - 3,
                        0,
                        0,
                        4));
    }

    @Test
    void iteratorRejectsMissingGapOverlapWrongAxisAndReverseOwnership() {
        FacePatchIterator.Cell negative = cell(FacePatchIterator.Axis.X, 0, 0, 0, 4);
        FacePatchIterator.Cell positive = cell(FacePatchIterator.Axis.X, 4, 0, 0, 4);
        FacePatchIterator.Cell gap = cell(FacePatchIterator.Axis.X, 8, 0, 0, 4);
        FacePatchIterator.Cell volumeOverlap = cell(FacePatchIterator.Axis.X, 0, 0, 0, 4);
        FacePatchIterator.Cell edgeOnly = cell(FacePatchIterator.Axis.X, 4, 4, 0, 4);

        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(FacePatchIterator.Axis.X, negative, gap));
        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(FacePatchIterator.Axis.X, negative, volumeOverlap));
        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(FacePatchIterator.Axis.X, negative, edgeOnly));
        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(FacePatchIterator.Axis.Y, negative, positive));
        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(FacePatchIterator.Axis.X, positive, negative));
        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(null, negative, positive));
        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(FacePatchIterator.Axis.X, null, positive));
        assertThrows(IllegalArgumentException.class, () ->
                FacePatchIterator.between(
                        FacePatchIterator.Axis.X, null, List.of(positive)));
    }

    @Test
    void iteratorRejectsOverlappingOrUnmatchedFacePartitions() {
        FacePatchIterator.Cell negativeA = cell(FacePatchIterator.Axis.X, 0, 0, 0, 4);
        FacePatchIterator.Cell negativeB = cell(FacePatchIterator.Axis.X, 0, 8, 0, 4);
        FacePatchIterator.Cell positiveA = cell(FacePatchIterator.Axis.X, 4, 0, 0, 4);

        assertThrows(IllegalArgumentException.class, () -> FacePatchIterator.between(
                FacePatchIterator.Axis.X,
                List.of(negativeA, negativeA),
                List.of(positiveA)));
        assertThrows(IllegalArgumentException.class, () -> FacePatchIterator.between(
                FacePatchIterator.Axis.X,
                List.of(negativeA, negativeB),
                List.of(positiveA)));
        assertThrows(IllegalArgumentException.class, () -> FacePatchIterator.between(
                FacePatchIterator.Axis.X,
                List.of(),
                List.of(positiveA)));
        assertThrows(IllegalArgumentException.class, () -> FacePatchIterator.between(
                FacePatchIterator.Axis.X,
                List.of(negativeA),
                java.util.Arrays.asList(positiveA, null)));
    }

    @Test
    void patchRecordRejectsTamperedAreaDistanceAndKey() {
        FacePatchIterator.FacePatch patch = FacePatchIterator.between(
                        FacePatchIterator.Axis.Z,
                        cell(FacePatchIterator.Axis.Z, 0, 0, 0, 8),
                        cell(FacePatchIterator.Axis.Z, 8, 0, 0, 4))
                .patches().get(0);

        assertThrows(IllegalArgumentException.class, () -> new FacePatchIterator.FacePatch(
                patch.key(), patch.negativeSideCell(), patch.positiveSideCell(),
                patch.edgeLengthBlocks(), patch.overlapAreaBlocksSquared() + 1,
                patch.centerDistanceBlocks()));
        assertThrows(IllegalArgumentException.class, () -> new FacePatchIterator.FacePatch(
                patch.key(), patch.negativeSideCell(), patch.positiveSideCell(),
                patch.edgeLengthBlocks(), patch.overlapAreaBlocksSquared(),
                patch.centerDistanceBlocks() + 1.0D));
        CanonicalFacePatchKey shiftedKey = CanonicalFacePatchKey.of(
                FacePatchIterator.Axis.Z,
                patch.key().worldX() + 4,
                patch.key().worldY(),
                patch.key().worldZ());
        assertThrows(IllegalArgumentException.class, () -> new FacePatchIterator.FacePatch(
                shiftedKey, patch.negativeSideCell(), patch.positiveSideCell(),
                patch.edgeLengthBlocks(), patch.overlapAreaBlocksSquared(),
                patch.centerDistanceBlocks()));
    }

    private static void assertPartition(
            FacePatchIterator iterator,
            Partition partition,
            FacePatchIterator.Axis axis,
            int interfacePlane,
            int expectedPatchCount,
            long expectedArea,
            double expectedDistance
    ) {
        assertEquals(expectedPatchCount, iterator.patchCount());
        assertEquals(expectedPatchCount,
                iterator.patches().stream().map(FacePatchIterator.FacePatch::key).distinct().count());
        assertEquals(expectedArea, iterator.patches().stream()
                .mapToLong(FacePatchIterator.FacePatch::overlapAreaBlocksSquared).sum());

        CanonicalFacePatchKey previous = null;
        for (FacePatchIterator.FacePatch patch : iterator) {
            assertEquals(axis, patch.key().axis());
            assertEquals(interfacePlane, normalCoordinate(axis, patch.key()));
            assertEquals(expectedDistance, patch.centerDistanceBlocks());
            assertTrue(partition.negative().contains(patch.negativeSideCell()));
            assertTrue(partition.positive().contains(patch.positiveSideCell()));
            assertEquals(0, Math.floorMod(patch.key().worldX(), 4));
            assertEquals(0, Math.floorMod(patch.key().worldY(), 4));
            assertEquals(0, Math.floorMod(patch.key().worldZ(), 4));
            if (previous != null) {
                assertTrue(previous.compareTo(patch.key()) < 0);
            }
            previous = patch.key();
        }
    }

    private static Partition partition(
            FacePatchIterator.Axis axis,
            int coarseWidth,
            int fineWidth,
            int interfacePlane,
            int minimumU,
            int minimumV,
            CoarseSide coarseSide
    ) {
        FacePatchIterator.Cell coarseNegative = cell(
                axis, interfacePlane - coarseWidth, minimumU, minimumV, coarseWidth);
        FacePatchIterator.Cell coarsePositive = cell(
                axis, interfacePlane, minimumU, minimumV, coarseWidth);
        List<FacePatchIterator.Cell> negativeFine = fineCells(
                axis, interfacePlane - fineWidth,
                minimumU, minimumV, coarseWidth, fineWidth);
        List<FacePatchIterator.Cell> positiveFine = fineCells(
                axis, interfacePlane,
                minimumU, minimumV, coarseWidth, fineWidth);
        return coarseSide == CoarseSide.NEGATIVE
                ? new Partition(List.of(coarseNegative), positiveFine)
                : new Partition(negativeFine, List.of(coarsePositive));
    }

    private static List<FacePatchIterator.Cell> fineCells(
            FacePatchIterator.Axis axis,
            int normalMinimum,
            int minimumU,
            int minimumV,
            int coarseWidth,
            int fineWidth
    ) {
        List<FacePatchIterator.Cell> cells = new ArrayList<>();
        for (int v = minimumV; v < minimumV + coarseWidth; v += fineWidth) {
            for (int u = minimumU; u < minimumU + coarseWidth; u += fineWidth) {
                cells.add(cell(axis, normalMinimum, u, v, fineWidth));
            }
        }
        return List.copyOf(cells);
    }

    private static FacePatchIterator.Cell cell(
            FacePatchIterator.Axis axis,
            int normalMinimum,
            int minimumU,
            int minimumV,
            int width
    ) {
        return switch (axis) {
            case X -> new FacePatchIterator.Cell(normalMinimum, minimumU, minimumV, width);
            case Y -> new FacePatchIterator.Cell(minimumU, normalMinimum, minimumV, width);
            case Z -> new FacePatchIterator.Cell(minimumU, minimumV, normalMinimum, width);
        };
    }

    private static int normalCoordinate(
            FacePatchIterator.Axis axis,
            CanonicalFacePatchKey key
    ) {
        return switch (axis) {
            case X -> key.worldX();
            case Y -> key.worldY();
            case Z -> key.worldZ();
        };
    }

    private enum CoarseSide {
        NEGATIVE,
        POSITIVE
    }

    private record Partition(
            List<FacePatchIterator.Cell> negative,
            List<FacePatchIterator.Cell> positive
    ) {
    }
}
