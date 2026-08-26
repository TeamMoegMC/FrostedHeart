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

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles regular and mixed Air-Air interfaces from published Page coverage.
 * Only +X/+Y/+Z faces are visited, so the world negative-axis support owns
 * every pair. The resulting sweep stores primitive operations, not mesh edges.
 */
public final class ImplicitAirAdjacency {
    private static final double APERTURE_BIT_AREA = 1.0D / 16.0D;
    private static final Axis[] AXES = Axis.values();

    private ImplicitAirAdjacency() {
    }

    public enum Axis {
        X,
        Y,
        Z
    }

    public record PageView(
            ThermalPage page,
            int pageSlot,
            int worldMinX,
            int worldMinY,
            int worldMinZ
    ) {
        public PageView {
            if (page == null) {
                throw new IllegalArgumentException("page is required");
            }
            if (pageSlot < 0) {
                throw new IllegalArgumentException("pageSlot must be non-negative");
            }
            if (Math.floorMod(worldMinX, 16) != 0
                    || Math.floorMod(worldMinY, 16) != 0
                    || Math.floorMod(worldMinZ, 16) != 0) {
                throw new IllegalArgumentException(
                        "Page world minimum must be aligned to 16 blocks");
            }
        }

        int worldMaxXExclusive() {
            return worldMinX + 16;
        }

        int worldMaxYExclusive() {
            return worldMinY + 16;
        }

        int worldMaxZExclusive() {
            return worldMinZ + 16;
        }
    }

    public record PositiveNeighbors(
            PageView positiveX,
            PageView positiveY,
            PageView positiveZ
    ) {
        private PageView forAxis(Axis axis) {
            return switch (axis) {
                case X -> positiveX;
                case Y -> positiveY;
                case Z -> positiveZ;
            };
        }
    }

    public record CompiledPairs(
            List<ThermalSweep.PairOperation> operations,
            double totalOpenAreaBlocksSquared,
            int mixedPairCount,
            int unavailablePositivePages,
            boolean ownerPublicationCurrent
    ) {
        public CompiledPairs {
            operations = List.copyOf(operations);
        }
    }

    /** Compiles only interfaces owned by one 4-cubed base Brick. */
    public static CompiledPairs compileOwnedBrickPairs(
            PageView owner,
            PositiveNeighbors neighbors,
            ThermalCellArena arena,
            int baseBrickIndex,
            double effectiveMixingWPerBlockK,
            double minimumMixedFaceDistanceBlocks,
            boolean applyBuoyancy
    ) {
        if (owner == null || neighbors == null || arena == null) {
            throw new IllegalArgumentException("owner, neighbors, and arena are required");
        }
        if (baseBrickIndex < 0 || baseBrickIndex >= ThermalPage.BASE_BRICK_COUNT) {
            throw new IllegalArgumentException("baseBrickIndex must be within [0, 63]");
        }
        requirePositiveFinite("effectiveMixingWPerBlockK", effectiveMixingWPerBlockK);
        requirePositiveFinite(
                "minimumMixedFaceDistanceBlocks", minimumMixedFaceDistanceBlocks);
        validatePositiveNeighbor(owner, neighbors.positiveX(), Axis.X);
        validatePositiveNeighbor(owner, neighbors.positiveY(), Axis.Y);
        validatePositiveNeighbor(owner, neighbors.positiveZ(), Axis.Z);

        ThermalPage.MutableCoverageQuery ownerProbe = new ThermalPage.MutableCoverageQuery();
        if (!owner.page().tryQueryPublishedCoverage(0, 0, 0, ownerProbe)) {
            return new CompiledPairs(List.of(), 0.0D, 0, 0, false);
        }

        boolean[] neighborAvailable = new boolean[AXES.length];
        int unavailablePositivePages = 0;
        for (Axis axis : AXES) {
            PageView neighbor = neighbors.forAxis(axis);
            if (neighbor == null) {
                continue;
            }
            ThermalPage.MutableCoverageQuery probe = new ThermalPage.MutableCoverageQuery();
            boolean available = neighbor.page().tryQueryPublishedCoverage(0, 0, 0, probe);
            neighborAvailable[axis.ordinal()] = available;
            if (!available) {
                unavailablePositivePages++;
            }
        }

        Long2ObjectMap<PairAccumulator> pairs = new Long2ObjectLinkedOpenHashMap<>();
        compileBaseBrick(
                owner, neighbors, neighborAvailable, arena, baseBrickIndex,
                minimumMixedFaceDistanceBlocks, pairs);

        List<ThermalSweep.PairOperation> operations = new ArrayList<>(pairs.size());
        double totalArea = 0.0D;
        int mixedPairs = 0;
        for (PairAccumulator pair : pairs.values()) {
            double conductance = effectiveMixingWPerBlockK
                    * pair.openAreaBlocksSquared / pair.centerDistanceBlocks;
            if (!Double.isFinite(conductance) || conductance < 0.0D) {
                throw new IllegalStateException("compiled Air-Air conductance is invalid");
            }
            operations.add(applyBuoyancy
                    ? ThermalSweep.PairOperation.buoyant(
                            pair.negativeCellSlot,
                            pair.positiveCellSlot,
                            conductance,
                            arena.centerY(pair.negativeCellSlot),
                            arena.centerY(pair.positiveCellSlot))
                    : ThermalSweep.PairOperation.fixed(
                            pair.negativeCellSlot,
                            pair.positiveCellSlot,
                            conductance));
            totalArea += pair.openAreaBlocksSquared;
            if (pair.mixed) {
                mixedPairs++;
            }
        }
        return new CompiledPairs(
                operations, totalArea, mixedPairs,
                unavailablePositivePages, true);
    }

    private static void compileBaseBrick(
            PageView owner,
            PositiveNeighbors neighbors,
            boolean[] neighborAvailable,
            ThermalCellArena arena,
            int baseIndex,
            double minimumMixedFaceDistanceBlocks,
            Long2ObjectMap<PairAccumulator> pairs
    ) {
        int brickX = baseIndex & 3;
        int brickZ = (baseIndex >>> 2) & 3;
        int brickY = (baseIndex >>> 4) & 3;
        ThermalPage.MutableCoverageQuery query = new ThermalPage.MutableCoverageQuery();
        if (!owner.page().tryQueryPublishedCoverage(
                brickX << 2, brickY << 2, brickZ << 2, query)) {
            throw new IllegalStateException("Page publication changed during pair compilation");
        }
        int negativeSupport = query.coverageRef();
        if (negativeSupport == ThermalPage.NO_COVERAGE) {
            return;
        }
        requireOwnedCell(owner, arena, negativeSupport);
        if (arena.supportRef(negativeSupport) != negativeSupport) {
            throw new IllegalStateException("published coverage does not reference a support");
        }
        if (arena.minimumX(negativeSupport) != owner.worldMinX() + (brickX << 2)
                || arena.minimumY(negativeSupport) != owner.worldMinY() + (brickY << 2)
                || arena.minimumZ(negativeSupport) != owner.worldMinZ() + (brickZ << 2)) {
            return;
        }
        for (Axis axis : AXES) {
            PageView positivePage = positivePageForSupportFace(
                    owner, neighbors, neighborAvailable, arena, negativeSupport, axis);
            if (positivePage == null) {
                continue;
            }
            int[] positiveSupports = collectPositiveSupports(
                    positivePage, arena, negativeSupport, axis);
            for (int positiveSupport : positiveSupports) {
                compileInterface(
                        arena, negativeSupport, positiveSupport, axis,
                        minimumMixedFaceDistanceBlocks, pairs);
            }
        }
    }

    private static void compileInterface(
            ThermalCellArena arena,
            int negativeSupport,
            int positiveSupport,
            Axis axis,
            double minimumMixedDistance,
            Long2ObjectMap<PairAccumulator> pairs
    ) {
        boolean negativeMixed = arena.isMixedSupport(negativeSupport);
        boolean positiveMixed = arena.isMixedSupport(positiveSupport);
        if (!negativeMixed && !positiveMixed) {
            double area = regularFaceOverlapArea(
                    arena, negativeSupport, positiveSupport, axis);
            addPair(
                    arena, negativeSupport, positiveSupport, axis,
                    area, minimumMixedDistance, false, pairs);
            return;
        }

        if (negativeMixed && positiveMixed) {
            compileMixedToMixed(
                    arena, negativeSupport, positiveSupport,
                    axis, minimumMixedDistance, pairs);
            return;
        }

        int mixedSupport = negativeMixed ? negativeSupport : positiveSupport;
        int regularSlot = negativeMixed ? positiveSupport : negativeSupport;
        ConservativeAirGeometry.Face mixedFace = faceFor(axis, negativeMixed);
        ComponentBrickCompiler.CompiledBrick geometry = arena.mixedGeometry(mixedSupport);
        for (int port = 0; port < geometry.facePortCount(); port++) {
            if (geometry.facePortFace(port) != mixedFace) {
                continue;
            }
            int componentSlot = arena.mixedComponentSlot(
                    mixedSupport, geometry.facePortComponentId(port));
            int negativeCell = negativeMixed ? componentSlot : regularSlot;
            int positiveCell = negativeMixed ? regularSlot : componentSlot;
            double area = Integer.bitCount(
                    geometry.facePortApertureMask(port)) * APERTURE_BIT_AREA;
            addPair(
                    arena, negativeCell, positiveCell, axis,
                    area, minimumMixedDistance, true, pairs);
        }
    }

    private static void compileMixedToMixed(
            ThermalCellArena arena,
            int negativeSupport,
            int positiveSupport,
            Axis axis,
            double minimumMixedDistance,
            Long2ObjectMap<PairAccumulator> pairs
    ) {
        ComponentBrickCompiler.CompiledBrick negative = arena.mixedGeometry(negativeSupport);
        ComponentBrickCompiler.CompiledBrick positive = arena.mixedGeometry(positiveSupport);
        ConservativeAirGeometry.Face negativeFace = faceFor(axis, true);
        ConservativeAirGeometry.Face positiveFace = faceFor(axis, false);
        for (int left = 0; left < negative.facePortCount(); left++) {
            if (negative.facePortFace(left) != negativeFace) {
                continue;
            }
            for (int right = 0; right < positive.facePortCount(); right++) {
                if (positive.facePortFace(right) != positiveFace
                        || negative.facePortBlockSlot(left)
                        != positive.facePortBlockSlot(right)) {
                    continue;
                }
                int aperture = negative.facePortApertureMask(left)
                        & positive.facePortApertureMask(right);
                if (aperture == 0) {
                    continue;
                }
                int negativeCell = arena.mixedComponentSlot(
                        negativeSupport, negative.facePortComponentId(left));
                int positiveCell = arena.mixedComponentSlot(
                        positiveSupport, positive.facePortComponentId(right));
                addPair(
                        arena, negativeCell, positiveCell, axis,
                        Integer.bitCount(aperture) * APERTURE_BIT_AREA,
                        minimumMixedDistance, true, pairs);
            }
        }
    }

    private static void addPair(
            ThermalCellArena arena,
            int negativeCell,
            int positiveCell,
            Axis axis,
            double openArea,
            double minimumMixedDistance,
            boolean mixed,
            Long2ObjectMap<PairAccumulator> pairs
    ) {
        if (!Double.isFinite(openArea) || openArea <= 0.0D) {
            throw new IllegalStateException("Air-Air open area must be finite and positive");
        }
        if (negativeCell == positiveCell) {
            throw new IllegalStateException("an Air-Air interface cannot connect a cell to itself");
        }
        double plane = supportMaximum(arena, arena.supportRef(negativeCell), axis);
        double negativeDistance = plane - center(arena, negativeCell, axis);
        double positiveDistance = center(arena, positiveCell, axis) - plane;
        if (arena.isMixedComponent(negativeCell)) {
            negativeDistance = Math.max(minimumMixedDistance, negativeDistance);
        }
        if (arena.isMixedComponent(positiveCell)) {
            positiveDistance = Math.max(minimumMixedDistance, positiveDistance);
        }
        double centerDistance = negativeDistance + positiveDistance;
        if (!Double.isFinite(centerDistance) || centerDistance <= 0.0D) {
            throw new IllegalStateException("Air-Air centers do not straddle their interface");
        }
        long key = ((long) negativeCell << 32) | (positiveCell & 0xffff_ffffL);
        PairAccumulator existing = pairs.get(key);
        if (existing == null) {
            existing = new PairAccumulator(
                    negativeCell, positiveCell, axis,
                    centerDistance, mixed);
            pairs.put(key, existing);
        } else if (existing.axis != axis
                || Math.abs(existing.centerDistanceBlocks - centerDistance) > 1.0e-12D) {
            throw new IllegalStateException("one cell pair compiled with inconsistent geometry");
        }
        existing.openAreaBlocksSquared += openArea;
    }

    private static int[] collectPositiveSupports(
            PageView positivePage,
            ThermalCellArena arena,
            int negativeSupport,
            Axis axis
    ) {
        int width = supportWidth(arena, negativeSupport);
        int[] unique = new int[16];
        int uniqueCount = 0;
        ThermalPage.MutableCoverageQuery query = new ThermalPage.MutableCoverageQuery();
        for (int v = 0; v < width; v += 4) {
            for (int u = 0; u < width; u += 4) {
                WorldBlock sample = positiveSample(arena, negativeSupport, axis, u, v);
                int localX = sample.x() - positivePage.worldMinX();
                int localY = sample.y() - positivePage.worldMinY();
                int localZ = sample.z() - positivePage.worldMinZ();
                if (!positivePage.page().tryQueryPublishedCoverage(
                        localX, localY, localZ, query)) {
                    throw new IllegalStateException(
                            "Page publication changed during pair compilation");
                }
                int supportRef = query.coverageRef();
                if (supportRef == ThermalPage.NO_COVERAGE) {
                    continue;
                }
                requireCoverageSupport(
                        positivePage, query, arena, supportRef, sample, axis);
                if (!contains(unique, uniqueCount, supportRef)) {
                    unique[uniqueCount++] = supportRef;
                }
            }
        }
        return java.util.Arrays.copyOf(unique, uniqueCount);
    }

    private static void requireCoverageSupport(
            PageView page,
            ThermalPage.MutableCoverageQuery query,
            ThermalCellArena arena,
            int supportRef,
            WorldBlock sample,
            Axis axis
    ) {
        if (!arena.isLive(supportRef)
                || arena.supportRef(supportRef) != supportRef
                || arena.pageSlot(supportRef) != page.pageSlot()) {
            throw new IllegalStateException("published coverage has invalid arena ownership");
        }
        boolean mixed = arena.isMixedSupport(supportRef);
        boolean pageMarksMixed = (page.page().mixedBrickMask()
                & (1L << query.baseBrickIndex())) != 0L;
        if (mixed != pageMarksMixed) {
            throw new IllegalStateException("Page mixed mask disagrees with its coverage support");
        }
        if (!containsBlock(arena, supportRef, sample)) {
            throw new IllegalStateException("published coverage does not cover its sample");
        }
        int positiveMinimum = supportMinimum(arena, supportRef, axis);
        int sampleNormal = coordinate(sample, axis);
        if (positiveMinimum != sampleNormal) {
            throw new IllegalStateException(
                    "positive support does not start on the owned interface plane");
        }
    }

    private static PageView positivePageForSupportFace(
            PageView owner,
            PositiveNeighbors neighbors,
            boolean[] neighborAvailable,
            ThermalCellArena arena,
            int negativeSupport,
            Axis axis
    ) {
        int plane = supportMaximum(arena, negativeSupport, axis);
        int pageMaximum = switch (axis) {
            case X -> owner.worldMaxXExclusive();
            case Y -> owner.worldMaxYExclusive();
            case Z -> owner.worldMaxZExclusive();
        };
        if (plane < pageMaximum) {
            return owner;
        }
        if (plane > pageMaximum) {
            throw new IllegalStateException("thermal support extends outside its owning Page");
        }
        PageView neighbor = neighbors.forAxis(axis);
        return neighbor != null && neighborAvailable[axis.ordinal()] ? neighbor : null;
    }

    private static WorldBlock positiveSample(
            ThermalCellArena arena,
            int negativeSupport,
            Axis axis,
            int u,
            int v
    ) {
        int minX = arena.minimumX(negativeSupport);
        int minY = arena.minimumY(negativeSupport);
        int minZ = arena.minimumZ(negativeSupport);
        int width = supportWidth(arena, negativeSupport);
        return switch (axis) {
            case X -> new WorldBlock(minX + width, minY + u, minZ + v);
            case Y -> new WorldBlock(minX + u, minY + width, minZ + v);
            case Z -> new WorldBlock(minX + u, minY + v, minZ + width);
        };
    }

    private static double regularFaceOverlapArea(
            ThermalCellArena arena,
            int negative,
            int positive,
            Axis axis
    ) {
        int first;
        int second;
        if (axis == Axis.X) {
            first = overlapLength(
                    arena.minimumY(negative), arena.minimumY(negative) + 4,
                    arena.minimumY(positive), arena.minimumY(positive) + 4);
            second = overlapLength(
                    arena.minimumZ(negative), arena.minimumZ(negative) + 4,
                    arena.minimumZ(positive), arena.minimumZ(positive) + 4);
        } else if (axis == Axis.Y) {
            first = overlapLength(
                    arena.minimumX(negative), arena.minimumX(negative) + 4,
                    arena.minimumX(positive), arena.minimumX(positive) + 4);
            second = overlapLength(
                    arena.minimumZ(negative), arena.minimumZ(negative) + 4,
                    arena.minimumZ(positive), arena.minimumZ(positive) + 4);
        } else {
            first = overlapLength(
                    arena.minimumX(negative), arena.minimumX(negative) + 4,
                    arena.minimumX(positive), arena.minimumX(positive) + 4);
            second = overlapLength(
                    arena.minimumY(negative), arena.minimumY(negative) + 4,
                    arena.minimumY(positive), arena.minimumY(positive) + 4);
        }
        double area = (double) first * second;
        if (area <= 0.0D) {
            throw new IllegalStateException("regular Air-Air supports do not overlap on their face");
        }
        return area;
    }

    private static int overlapLength(int minA, int maxA, int minB, int maxB) {
        return Math.max(0, Math.min(maxA, maxB) - Math.max(minA, minB));
    }

    private static void requireOwnedCell(
            PageView page,
            ThermalCellArena arena,
            int slot
    ) {
        if (!arena.isLive(slot) || arena.pageSlot(slot) != page.pageSlot()) {
            throw new IllegalStateException("Page cell span contains invalid ownership");
        }
        int width = supportWidth(arena, arena.supportRef(slot));
        int minX = arena.minimumX(slot);
        int minY = arena.minimumY(slot);
        int minZ = arena.minimumZ(slot);
        if (minX < page.worldMinX() || minX + width > page.worldMaxXExclusive()
                || minY < page.worldMinY() || minY + width > page.worldMaxYExclusive()
                || minZ < page.worldMinZ() || minZ + width > page.worldMaxZExclusive()) {
            throw new IllegalStateException("thermal cell lies outside its Page world bounds");
        }
    }

    private static int supportWidth(ThermalCellArena arena, int supportRef) {
        return 4;
    }

    private static int supportMinimum(
            ThermalCellArena arena,
            int supportRef,
            Axis axis
    ) {
        return switch (axis) {
            case X -> arena.minimumX(supportRef);
            case Y -> arena.minimumY(supportRef);
            case Z -> arena.minimumZ(supportRef);
        };
    }

    private static int supportMaximum(
            ThermalCellArena arena,
            int supportRef,
            Axis axis
    ) {
        return supportMinimum(arena, supportRef, axis) + supportWidth(arena, supportRef);
    }

    private static double center(
            ThermalCellArena arena,
            int slot,
            Axis axis
    ) {
        return switch (axis) {
            case X -> arena.centerX(slot);
            case Y -> arena.centerY(slot);
            case Z -> arena.centerZ(slot);
        };
    }

    private static ConservativeAirGeometry.Face faceFor(
            Axis axis,
            boolean positive
    ) {
        return switch (axis) {
            case X -> positive
                    ? ConservativeAirGeometry.Face.POSITIVE_X
                    : ConservativeAirGeometry.Face.NEGATIVE_X;
            case Y -> positive
                    ? ConservativeAirGeometry.Face.POSITIVE_Y
                    : ConservativeAirGeometry.Face.NEGATIVE_Y;
            case Z -> positive
                    ? ConservativeAirGeometry.Face.POSITIVE_Z
                    : ConservativeAirGeometry.Face.NEGATIVE_Z;
        };
    }

    private static boolean containsBlock(
            ThermalCellArena arena,
            int supportRef,
            WorldBlock block
    ) {
        int width = supportWidth(arena, supportRef);
        return block.x() >= arena.minimumX(supportRef)
                && block.x() < arena.minimumX(supportRef) + width
                && block.y() >= arena.minimumY(supportRef)
                && block.y() < arena.minimumY(supportRef) + width
                && block.z() >= arena.minimumZ(supportRef)
                && block.z() < arena.minimumZ(supportRef) + width;
    }

    private static int coordinate(WorldBlock block, Axis axis) {
        return switch (axis) {
            case X -> block.x();
            case Y -> block.y();
            case Z -> block.z();
        };
    }

    private static boolean contains(int[] values, int length, int candidate) {
        for (int index = 0; index < length; index++) {
            if (values[index] == candidate) {
                return true;
            }
        }
        return false;
    }

    private static void validatePositiveNeighbor(
            PageView owner,
            PageView neighbor,
            Axis axis
    ) {
        if (neighbor == null) {
            return;
        }
        int expectedX = owner.worldMinX() + (axis == Axis.X ? 16 : 0);
        int expectedY = owner.worldMinY() + (axis == Axis.Y ? 16 : 0);
        int expectedZ = owner.worldMinZ() + (axis == Axis.Z ? 16 : 0);
        if (neighbor.worldMinX() != expectedX
                || neighbor.worldMinY() != expectedY
                || neighbor.worldMinZ() != expectedZ) {
            throw new IllegalArgumentException(
                    axis + " neighbor is not the adjacent positive-axis Page");
        }
    }

    private static void requirePositiveFinite(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static final class PairAccumulator {
        private final int negativeCellSlot;
        private final int positiveCellSlot;
        private final Axis axis;
        private final double centerDistanceBlocks;
        private final boolean mixed;
        private double openAreaBlocksSquared;

        private PairAccumulator(
                int negativeCellSlot,
                int positiveCellSlot,
                Axis axis,
                double centerDistanceBlocks,
                boolean mixed
        ) {
            this.negativeCellSlot = negativeCellSlot;
            this.positiveCellSlot = positiveCellSlot;
            this.axis = axis;
            this.centerDistanceBlocks = centerDistanceBlocks;
            this.mixed = mixed;
        }
    }

    private record WorldBlock(int x, int y, int z) {
    }
}
