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

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java conservative air complement prototype for one Minecraft block.
 * A quarter-block microcell is considered air only when no blocker intersects
 * any part of its volume. This can close a real narrow opening, but cannot
 * manufacture an opening through supplied blocker geometry.
 */
public final class ConservativeAirGeometry {
    public static final int GRID_SIZE = 4;
    public static final int MICROCELL_COUNT = GRID_SIZE * GRID_SIZE * GRID_SIZE;
    public static final int FULL_FACE_MASK = 0xffff;
    private static final double MICROCELL_SIZE = 1.0D / GRID_SIZE;

    private ConservativeAirGeometry() {
    }

    public enum Face {
        NEGATIVE_X,
        POSITIVE_X,
        NEGATIVE_Y,
        POSITIVE_Y,
        NEGATIVE_Z,
        POSITIVE_Z;

        public static final int COUNT = 6;

        public static Face fromOrdinal(int ordinal) {
            return switch (ordinal) {
                case 0 -> NEGATIVE_X;
                case 1 -> POSITIVE_X;
                case 2 -> NEGATIVE_Y;
                case 3 -> POSITIVE_Y;
                case 4 -> NEGATIVE_Z;
                case 5 -> POSITIVE_Z;
                default -> throw new IllegalArgumentException("face ordinal is out of bounds");
            };
        }
    }

    public enum Status {
        RESOLVED,
        CONSERVATIVE_UNSUPPORTED
    }

    /** Axis-aligned blocker in block-local coordinates, inclusive of 0 and 1. */
    public record UnitBox(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        public UnitBox {
            requireCoordinate("minX", minX);
            requireCoordinate("minY", minY);
            requireCoordinate("minZ", minZ);
            requireCoordinate("maxX", maxX);
            requireCoordinate("maxY", maxY);
            requireCoordinate("maxZ", maxZ);
            if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("UnitBox must have positive volume");
            }
        }

        public static UnitBox fullBlock() {
            return new UnitBox(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        }
    }

    /**
     * Face bits use row-major quarter tiles. X faces use (u=z,v=y), Y faces
     * use (u=x,v=z), and Z faces use (u=x,v=y).
     */
    public record AirComponent(
            int id,
            long microcellMask,
            int negativeXMask,
            int positiveXMask,
            int negativeYMask,
            int positiveYMask,
            int negativeZMask,
            int positiveZMask
    ) {
        public AirComponent {
            if (id < 0) {
                throw new IllegalArgumentException("component id must be non-negative");
            }
            if (microcellMask == 0L) {
                throw new IllegalArgumentException("component mask must be non-empty");
            }
            requireFaceMask("negativeXMask", negativeXMask);
            requireFaceMask("positiveXMask", positiveXMask);
            requireFaceMask("negativeYMask", negativeYMask);
            requireFaceMask("positiveYMask", positiveYMask);
            requireFaceMask("negativeZMask", negativeZMask);
            requireFaceMask("positiveZMask", positiveZMask);
        }

        public int faceMask(Face face) {
            if (face == null) {
                throw new IllegalArgumentException("face is required");
            }
            return switch (face) {
                case NEGATIVE_X -> negativeXMask;
                case POSITIVE_X -> positiveXMask;
                case NEGATIVE_Y -> negativeYMask;
                case POSITIVE_Y -> positiveYMask;
                case NEGATIVE_Z -> negativeZMask;
                case POSITIVE_Z -> positiveZMask;
            };
        }

    }

    public record Resolution(
            Status status,
            List<AirComponent> components
    ) {
        public Resolution {
            if (status == null || components == null) {
                throw new IllegalArgumentException("resolution fields are required");
            }
            components = List.copyOf(components);
            if (status == Status.CONSERVATIVE_UNSUPPORTED
                    && !components.isEmpty()) {
                throw new IllegalArgumentException(
                        "unsupported geometry cannot expose components");
            }
        }

        public long provenAirMicrocellMask() {
            long mask = 0L;
            for (int index = 0, size = components.size(); index < size; index++) {
                mask |= components.get(index).microcellMask();
            }
            return mask;
        }
    }

    public static Resolution resolve(List<UnitBox> blockers, int maximumRegions) {
        if (blockers == null) {
            throw new IllegalArgumentException("blockers are required");
        }
        if (maximumRegions <= 0) {
            throw new IllegalArgumentException("maximumRegions must be positive");
        }
        for (int index = 0, size = blockers.size(); index < size; index++) {
            UnitBox blocker = blockers.get(index);
            if (blocker == null) {
                throw new IllegalArgumentException("blockers must not contain null");
            }
        }
        List<UnitBox> blockerSnapshot = List.copyOf(blockers);
        long blockedMask = 0L;
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                for (int x = 0; x < GRID_SIZE; x++) {
                    if (isBlocked(blockerSnapshot, x, y, z)) {
                        blockedMask |= bit(x, y, z);
                    }
                }
            }
        }

        List<Long> componentMasks = findComponents(blockedMask);
        if (componentMasks.size() > maximumRegions) {
            return new Resolution(
                    Status.CONSERVATIVE_UNSUPPORTED,
                    List.of()
            );
        }

        List<AirComponent> components = new ArrayList<>(componentMasks.size());
        for (int id = 0; id < componentMasks.size(); id++) {
            components.add(toComponent(id, componentMasks.get(id)));
        }
        return new Resolution(
                Status.RESOLVED,
                components
        );
    }

    private static List<Long> findComponents(long blockedMask) {
        List<Long> components = new ArrayList<>();
        long visited = blockedMask;
        int[] queue = new int[MICROCELL_COUNT];
        for (int seed = 0; seed < MICROCELL_COUNT; seed++) {
            long seedBit = 1L << seed;
            if ((visited & seedBit) != 0L) {
                continue;
            }
            int read = 0;
            int write = 0;
            queue[write++] = seed;
            visited |= seedBit;
            long componentMask = 0L;
            while (read < write) {
                int current = queue[read++];
                componentMask |= 1L << current;
                int x = current & 3;
                int z = (current >>> 2) & 3;
                int y = (current >>> 4) & 3;
                if (x > 0) {
                    write = enqueue(current - 1, visited, queue, write);
                    visited |= 1L << (current - 1);
                }
                if (x + 1 < GRID_SIZE) {
                    write = enqueue(current + 1, visited, queue, write);
                    visited |= 1L << (current + 1);
                }
                if (z > 0) {
                    write = enqueue(current - GRID_SIZE, visited, queue, write);
                    visited |= 1L << (current - GRID_SIZE);
                }
                if (z + 1 < GRID_SIZE) {
                    write = enqueue(current + GRID_SIZE, visited, queue, write);
                    visited |= 1L << (current + GRID_SIZE);
                }
                if (y > 0) {
                    write = enqueue(current - GRID_SIZE * GRID_SIZE, visited, queue, write);
                    visited |= 1L << (current - GRID_SIZE * GRID_SIZE);
                }
                if (y + 1 < GRID_SIZE) {
                    write = enqueue(current + GRID_SIZE * GRID_SIZE, visited, queue, write);
                    visited |= 1L << (current + GRID_SIZE * GRID_SIZE);
                }
            }
            components.add(componentMask);
        }
        return components;
    }

    private static int enqueue(int index, long visited, int[] queue, int write) {
        if ((visited & (1L << index)) == 0L) {
            queue[write++] = index;
        }
        return write;
    }

    private static AirComponent toComponent(int id, long mask) {
        int negativeX = 0;
        int positiveX = 0;
        int negativeY = 0;
        int positiveY = 0;
        int negativeZ = 0;
        int positiveZ = 0;
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                for (int x = 0; x < GRID_SIZE; x++) {
                    if ((mask & bit(x, y, z)) == 0L) {
                        continue;
                    }
                    if (x == 0) {
                        negativeX |= 1 << faceBit(z, y);
                    }
                    if (x == GRID_SIZE - 1) {
                        positiveX |= 1 << faceBit(z, y);
                    }
                    if (y == 0) {
                        negativeY |= 1 << faceBit(x, z);
                    }
                    if (y == GRID_SIZE - 1) {
                        positiveY |= 1 << faceBit(x, z);
                    }
                    if (z == 0) {
                        negativeZ |= 1 << faceBit(x, y);
                    }
                    if (z == GRID_SIZE - 1) {
                        positiveZ |= 1 << faceBit(x, y);
                    }
                }
            }
        }
        return new AirComponent(
                id,
                mask,
                negativeX,
                positiveX,
                negativeY,
                positiveY,
                negativeZ,
                positiveZ
        );
    }

    private static boolean isBlocked(List<UnitBox> blockers, int x, int y, int z) {
        double minX = x * MICROCELL_SIZE;
        double minY = y * MICROCELL_SIZE;
        double minZ = z * MICROCELL_SIZE;
        double maxX = minX + MICROCELL_SIZE;
        double maxY = minY + MICROCELL_SIZE;
        double maxZ = minZ + MICROCELL_SIZE;
        for (int index = 0, size = blockers.size(); index < size; index++) {
            UnitBox blocker = blockers.get(index);
            if (blocker.maxX() > minX && blocker.minX() < maxX
                    && blocker.maxY() > minY && blocker.minY() < maxY
                    && blocker.maxZ() > minZ && blocker.minZ() < maxZ) {
                return true;
            }
        }
        return false;
    }

    private static long bit(int x, int y, int z) {
        requireGridCoordinate("x", x);
        requireGridCoordinate("y", y);
        requireGridCoordinate("z", z);
        return 1L << ((y << 4) | (z << 2) | x);
    }

    private static int faceBit(int u, int v) {
        return (v << 2) | u;
    }

    private static void requireCoordinate(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and within [0, 1]");
        }
    }

    private static void requireGridCoordinate(String name, int value) {
        if (value < 0 || value >= GRID_SIZE) {
            throw new IllegalArgumentException(name + " must be within [0, 3]");
        }
    }

    private static void requireFaceMask(String name, int mask) {
        if ((mask & ~FULL_FACE_MASK) != 0) {
            throw new IllegalArgumentException(name + " must fit 16 bits");
        }
    }
}
