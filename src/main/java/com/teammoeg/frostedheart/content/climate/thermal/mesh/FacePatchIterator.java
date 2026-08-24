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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Decomposes two legal 4/8/16 cell face partitions into canonical overlaps.
 *
 * <pre>
 * world negative-axis cells --own and enumerate--&gt; canonical patches
 * world positive-axis cells ----------------------&gt; neighbors only
 * </pre>
 *
 * <p>The negative-side partition is always the owner, independently of which
 * side is coarse. Inputs are world-aligned cubes and every returned patch is
 * ordered by its world key. Mixed Brick ports and block-face aperture bits are
 * intentionally outside this iterator.</p>
 */
public final class FacePatchIterator implements Iterable<FacePatchIterator.FacePatch> {
    private final List<FacePatch> patches;

    private FacePatchIterator(List<FacePatch> patches) {
        this.patches = List.copyOf(patches);
    }

    public enum Axis {
        X,
        Y,
        Z;

        private int minimumNormal(Cell cell) {
            return switch (this) {
                case X -> cell.minX();
                case Y -> cell.minY();
                case Z -> cell.minZ();
            };
        }

        private int maximumNormal(Cell cell) {
            return Math.addExact(minimumNormal(cell), cell.width());
        }

        private int minimumU(Cell cell) {
            return switch (this) {
                case X -> cell.minY();
                case Y, Z -> cell.minX();
            };
        }

        private int minimumV(Cell cell) {
            return switch (this) {
                case X, Y -> cell.minZ();
                case Z -> cell.minY();
            };
        }

        private CanonicalFacePatchKey key(int plane, int minimumU, int minimumV) {
            return switch (this) {
                case X -> CanonicalFacePatchKey.of(this, plane, minimumU, minimumV);
                case Y -> CanonicalFacePatchKey.of(this, minimumU, plane, minimumV);
                case Z -> CanonicalFacePatchKey.of(this, minimumU, minimumV, plane);
            };
        }

        private int keyNormal(CanonicalFacePatchKey key) {
            return switch (this) {
                case X -> key.worldX();
                case Y -> key.worldY();
                case Z -> key.worldZ();
            };
        }

        private int keyU(CanonicalFacePatchKey key) {
            return switch (this) {
                case X -> key.worldY();
                case Y, Z -> key.worldX();
            };
        }

        private int keyV(CanonicalFacePatchKey key) {
            return switch (this) {
                case X, Y -> key.worldZ();
                case Z -> key.worldY();
            };
        }
    }

    /** A world-aligned V1 thermal cell support cube. */
    public record Cell(int minX, int minY, int minZ, int width) {
        public Cell {
            if (width != 4 && width != 8 && width != 16) {
                throw new IllegalArgumentException("width must be one of 4, 8, or 16");
            }
            if (Math.floorMod(minX, width) != 0
                    || Math.floorMod(minY, width) != 0
                    || Math.floorMod(minZ, width) != 0) {
                throw new IllegalArgumentException(
                        "cell minimum coordinates must be world-aligned to width");
            }
            int maxX;
            int maxY;
            int maxZ;
            try {
                maxX = Math.addExact(minX, width);
                maxY = Math.addExact(minY, width);
                maxZ = Math.addExact(minZ, width);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("cell bounds overflow", exception);
            }
            CanonicalFacePatchKey.requirePackableCoordinate(minX, minY, minZ);
            CanonicalFacePatchKey.requirePackableCoordinate(maxX, maxY, maxZ);
        }

        public double halfWidthBlocks() {
            return width * 0.5D;
        }
    }

    /** One exactly-once overlap owned by {@link #negativeSideCell()}. */
    public record FacePatch(
            CanonicalFacePatchKey key,
            Cell negativeSideCell,
            Cell positiveSideCell,
            int edgeLengthBlocks,
            long overlapAreaBlocksSquared,
            double centerDistanceBlocks
    ) {
        public FacePatch {
            if (key == null || negativeSideCell == null || positiveSideCell == null) {
                throw new IllegalArgumentException("patch key and cells are required");
            }
            Axis axis = key.axis();
            int plane = axis.maximumNormal(negativeSideCell);
            if (plane != axis.minimumNormal(positiveSideCell)
                    || plane != axis.keyNormal(key)) {
                throw new IllegalArgumentException(
                        "patch cells and key must share one adjacent interface plane");
            }
            int minimumU = Math.max(
                    axis.minimumU(negativeSideCell), axis.minimumU(positiveSideCell));
            int maximumU = Math.min(
                    axis.minimumU(negativeSideCell) + negativeSideCell.width(),
                    axis.minimumU(positiveSideCell) + positiveSideCell.width());
            int minimumV = Math.max(
                    axis.minimumV(negativeSideCell), axis.minimumV(positiveSideCell));
            int maximumV = Math.min(
                    axis.minimumV(negativeSideCell) + negativeSideCell.width(),
                    axis.minimumV(positiveSideCell) + positiveSideCell.width());
            int extentU = maximumU - minimumU;
            int extentV = maximumV - minimumV;
            if (extentU <= 0 || extentU != extentV || edgeLengthBlocks != extentU) {
                throw new IllegalArgumentException("patch must be one non-empty square overlap");
            }
            if (axis.keyU(key) != minimumU || axis.keyV(key) != minimumV) {
                throw new IllegalArgumentException(
                        "patch key must use the overlap's minimum world corner");
            }
            long expectedArea = Math.multiplyExact((long) extentU, extentV);
            if (overlapAreaBlocksSquared != expectedArea) {
                throw new IllegalArgumentException("overlap area does not match cell geometry");
            }
            double expectedDistance = negativeSideCell.halfWidthBlocks()
                    + positiveSideCell.halfWidthBlocks();
            if (Double.compare(centerDistanceBlocks, expectedDistance) != 0) {
                throw new IllegalArgumentException(
                        "center distance must equal the sum of normal half-widths");
            }
        }
    }

    public static FacePatchIterator between(
            Axis axis,
            Cell negativeSideCell,
            Cell positiveSideCell
    ) {
        if (negativeSideCell == null || positiveSideCell == null) {
            throw new IllegalArgumentException("both cells are required");
        }
        return between(axis, List.of(negativeSideCell), List.of(positiveSideCell));
    }

    public static FacePatchIterator between(
            Axis axis,
            List<Cell> negativeSideCells,
            List<Cell> positiveSideCells
    ) {
        if (axis == null) {
            throw new IllegalArgumentException("axis is required");
        }
        List<Cell> negative = copyCells("negativeSideCells", negativeSideCells);
        List<Cell> positive = copyCells("positiveSideCells", positiveSideCells);

        int interfacePlane = axis.maximumNormal(negative.get(0));
        requireSharedPlane(axis, negative, interfacePlane, true, "negativeSideCells");
        requireSharedPlane(axis, positive, interfacePlane, false, "positiveSideCells");
        requireNonOverlappingPartition(axis, negative, "negativeSideCells");
        requireNonOverlappingPartition(axis, positive, "positiveSideCells");

        boolean[] negativeTouched = new boolean[negative.size()];
        boolean[] positiveTouched = new boolean[positive.size()];
        Set<CanonicalFacePatchKey> keys = new HashSet<>();
        List<FacePatch> result = new ArrayList<>();
        for (int negativeIndex = 0; negativeIndex < negative.size(); negativeIndex++) {
            Cell negativeCell = negative.get(negativeIndex);
            for (int positiveIndex = 0; positiveIndex < positive.size(); positiveIndex++) {
                Cell positiveCell = positive.get(positiveIndex);
                int minimumU = Math.max(
                        axis.minimumU(negativeCell), axis.minimumU(positiveCell));
                int maximumU = Math.min(
                        axis.minimumU(negativeCell) + negativeCell.width(),
                        axis.minimumU(positiveCell) + positiveCell.width());
                int minimumV = Math.max(
                        axis.minimumV(negativeCell), axis.minimumV(positiveCell));
                int maximumV = Math.min(
                        axis.minimumV(negativeCell) + negativeCell.width(),
                        axis.minimumV(positiveCell) + positiveCell.width());
                int extentU = maximumU - minimumU;
                int extentV = maximumV - minimumV;
                if (extentU <= 0 || extentV <= 0) {
                    continue;
                }
                if (extentU != extentV) {
                    throw new IllegalArgumentException(
                            "world-aligned V1 cell faces must overlap as square patches");
                }

                CanonicalFacePatchKey key = axis.key(interfacePlane, minimumU, minimumV);
                if (!keys.add(key)) {
                    throw new IllegalArgumentException("face partitions produce duplicate patch key " + key);
                }
                long area = Math.multiplyExact((long) extentU, extentV);
                double distance = negativeCell.halfWidthBlocks()
                        + positiveCell.halfWidthBlocks();
                result.add(new FacePatch(
                        key, negativeCell, positiveCell, extentU, area, distance));
                negativeTouched[negativeIndex] = true;
                positiveTouched[positiveIndex] = true;
            }
        }

        requireEveryCellTouches("negativeSideCells", negativeTouched);
        requireEveryCellTouches("positiveSideCells", positiveTouched);
        result.sort((left, right) -> left.key().compareTo(right.key()));
        return new FacePatchIterator(result);
    }

    public int patchCount() {
        return patches.size();
    }

    public List<FacePatch> patches() {
        return patches;
    }

    @Override
    public Iterator<FacePatch> iterator() {
        return patches.iterator();
    }

    private static List<Cell> copyCells(String name, List<Cell> cells) {
        if (cells == null || cells.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one cell");
        }
        List<Cell> copy = new ArrayList<>(cells.size());
        for (int index = 0; index < cells.size(); index++) {
            Cell cell = cells.get(index);
            if (cell == null) {
                throw new IllegalArgumentException(name + "[" + index + "] is required");
            }
            copy.add(cell);
        }
        return List.copyOf(copy);
    }

    private static void requireSharedPlane(
            Axis axis,
            List<Cell> cells,
            int expectedPlane,
            boolean negativeSide,
            String name
    ) {
        for (int index = 0; index < cells.size(); index++) {
            Cell cell = cells.get(index);
            int plane = negativeSide
                    ? axis.maximumNormal(cell)
                    : axis.minimumNormal(cell);
            if (plane != expectedPlane) {
                throw new IllegalArgumentException(
                        name + "[" + index + "] is not on interface plane " + expectedPlane);
            }
        }
    }

    private static void requireNonOverlappingPartition(
            Axis axis,
            List<Cell> cells,
            String name
    ) {
        for (int first = 0; first < cells.size(); first++) {
            Cell cellA = cells.get(first);
            for (int second = first + 1; second < cells.size(); second++) {
                Cell cellB = cells.get(second);
                if (overlapLength(
                        axis.minimumU(cellA), cellA.width(),
                        axis.minimumU(cellB), cellB.width()) > 0
                        && overlapLength(
                        axis.minimumV(cellA), cellA.width(),
                        axis.minimumV(cellB), cellB.width()) > 0) {
                    throw new IllegalArgumentException(
                            name + " contains overlapping face cells at indices "
                                    + first + " and " + second);
                }
            }
        }
    }

    private static int overlapLength(int minimumA, int widthA, int minimumB, int widthB) {
        return Math.min(minimumA + widthA, minimumB + widthB)
                - Math.max(minimumA, minimumB);
    }

    private static void requireEveryCellTouches(String name, boolean[] touched) {
        for (int index = 0; index < touched.length; index++) {
            if (!touched[index]) {
                throw new IllegalArgumentException(
                        name + "[" + index + "] has no adjacent face overlap");
            }
        }
    }
}
