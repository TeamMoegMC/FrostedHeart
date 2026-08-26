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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Compiles 64 block-local air patterns into one bounded 4x4x4 component Brick. */
public final class ComponentBrickCompiler {
    public static final int BLOCKS_PER_AXIS = 4;
    public static final int BLOCK_COUNT = BLOCKS_PER_AXIS * BLOCKS_PER_AXIS * BLOCKS_PER_AXIS;

    private ComponentBrickCompiler() {
    }

    public enum Status {
        RESOLVED,
        CONSERVATIVE_UNSUPPORTED
    }

    public enum UnsupportedReason {
        NONE,
        BLOCK_INPUT_UNSUPPORTED,
        REGION_LIMIT_EXCEEDED
    }

    public record Compilation(
            Status status,
            UnsupportedReason unsupportedReason,
            int unsupportedBlockIndex,
            Optional<CompiledBrick> brick
    ) {
        public Compilation {
            if (status == null || unsupportedReason == null || brick == null) {
                throw new IllegalArgumentException("compilation fields are required");
            }
            if (status == Status.RESOLVED) {
                if (unsupportedReason != UnsupportedReason.NONE
                        || unsupportedBlockIndex != -1
                        || brick.isEmpty()) {
                    throw new IllegalArgumentException("resolved compilation must contain one Brick");
                }
            } else if (unsupportedReason == UnsupportedReason.NONE
                    || unsupportedBlockIndex < 0
                    || brick.isPresent()) {
                throw new IllegalArgumentException("unsupported compilation must identify one block");
            }
        }
    }

    /** Primitive-array correctness layout. Accessors expose scalar values only. */
    public static final class CompiledBrick {
        private final int[] blockAtomOffset;
        private final int[] atomCompiledComponentId;
        private final double[] componentVolume;
        private final double[] componentCentroidX;
        private final double[] componentCentroidY;
        private final double[] componentCentroidZ;
        private final byte[] facePortFace;
        private final byte[] facePortBlockSlot;
        private final int[] facePortComponent;
        private final int[] facePortAperture;

        private CompiledBrick(
                int[] blockAtomOffset,
                int[] atomCompiledComponentId,
                double[] componentVolume,
                double[] componentCentroidX,
                double[] componentCentroidY,
                double[] componentCentroidZ,
                byte[] facePortFace,
                byte[] facePortBlockSlot,
                int[] facePortComponent,
                int[] facePortAperture
        ) {
            this.blockAtomOffset = blockAtomOffset;
            this.atomCompiledComponentId = atomCompiledComponentId;
            this.componentVolume = componentVolume;
            this.componentCentroidX = componentCentroidX;
            this.componentCentroidY = componentCentroidY;
            this.componentCentroidZ = componentCentroidZ;
            this.facePortFace = facePortFace;
            this.facePortBlockSlot = facePortBlockSlot;
            this.facePortComponent = facePortComponent;
            this.facePortAperture = facePortAperture;
        }

        public int componentCount() {
            return componentVolume.length;
        }

        public int facePortCount() {
            return facePortFace.length;
        }

        public int compiledComponentAt(int blockIndex, int localRegionId) {
            requireBlockIndex(blockIndex);
            int atomCountForBlock = blockAtomOffset[blockIndex + 1] - blockAtomOffset[blockIndex];
            if (localRegionId < 0 || localRegionId >= atomCountForBlock) {
                return -1;
            }
            int atomIndex = blockAtomOffset[blockIndex] + localRegionId;
            return atomCompiledComponentId[atomIndex];
        }

        public double componentVolume(int componentId) {
            requireIndex("componentId", componentId, componentCount());
            return componentVolume[componentId];
        }

        public double componentCentroidX(int componentId) {
            requireIndex("componentId", componentId, componentCount());
            return componentCentroidX[componentId];
        }

        public double componentCentroidY(int componentId) {
            requireIndex("componentId", componentId, componentCount());
            return componentCentroidY[componentId];
        }

        public double componentCentroidZ(int componentId) {
            requireIndex("componentId", componentId, componentCount());
            return componentCentroidZ[componentId];
        }

        public ConservativeAirGeometry.Face facePortFace(int portIndex) {
            requireIndex("portIndex", portIndex, facePortCount());
            return ConservativeAirGeometry.Face.fromOrdinal(
                    Byte.toUnsignedInt(facePortFace[portIndex]));
        }

        public int facePortBlockSlot(int portIndex) {
            requireIndex("portIndex", portIndex, facePortCount());
            return Byte.toUnsignedInt(facePortBlockSlot[portIndex]);
        }

        public int facePortComponentId(int portIndex) {
            requireIndex("portIndex", portIndex, facePortCount());
            return facePortComponent[portIndex];
        }

        public int facePortApertureMask(int portIndex) {
            requireIndex("portIndex", portIndex, facePortCount());
            return facePortAperture[portIndex];
        }

    }

    public static Compilation compile(
            List<ConservativeAirGeometry.Resolution> blockGeometry,
            int maximumRegionsPerBlock
    ) {
        if (blockGeometry == null || blockGeometry.size() != BLOCK_COUNT) {
            throw new IllegalArgumentException("blockGeometry must contain exactly 64 entries");
        }
        if (maximumRegionsPerBlock <= 0) {
            throw new IllegalArgumentException("maximumRegionsPerBlock must be positive");
        }
        int[] blockAtomOffset = new int[BLOCK_COUNT + 1];
        for (int blockIndex = 0; blockIndex < BLOCK_COUNT; blockIndex++) {
            ConservativeAirGeometry.Resolution resolution = blockGeometry.get(blockIndex);
            if (resolution == null || resolution.status() != ConservativeAirGeometry.Status.RESOLVED) {
                return unsupported(UnsupportedReason.BLOCK_INPUT_UNSUPPORTED, blockIndex);
            }
            if (resolution.components().size() > maximumRegionsPerBlock) {
                return unsupported(UnsupportedReason.REGION_LIMIT_EXCEEDED, blockIndex);
            }
            blockAtomOffset[blockIndex + 1] =
                    blockAtomOffset[blockIndex] + resolution.components().size();
        }

        int atomCount = blockAtomOffset[BLOCK_COUNT];
        int maximumAtoms = Math.multiplyExact(BLOCK_COUNT, maximumRegionsPerBlock);
        if (atomCount > maximumAtoms) {
            return unsupported(UnsupportedReason.REGION_LIMIT_EXCEEDED, BLOCK_COUNT - 1);
        }
        int[] parent = new int[atomCount];
        for (int blockIndex = 0; blockIndex < BLOCK_COUNT; blockIndex++) {
            List<ConservativeAirGeometry.AirComponent> components =
                    blockGeometry.get(blockIndex).components();
            for (int localRegionId = 0; localRegionId < components.size(); localRegionId++) {
                if (components.get(localRegionId).id() != localRegionId) {
                    throw new IllegalArgumentException("block-local component IDs must be dense and ordered");
                }
                int atom = blockAtomOffset[blockIndex] + localRegionId;
                parent[atom] = atom;
            }
        }

        connectInteriorFaces(blockGeometry, blockAtomOffset, parent);

        int[] atomCompiledComponent = new int[atomCount];
        Map<Integer, Integer> compiledByRoot = new HashMap<>();
        for (int atom = 0; atom < atomCount; atom++) {
            int root = find(parent, atom);
            atomCompiledComponent[atom] =
                    compiledByRoot.computeIfAbsent(root, ignored -> compiledByRoot.size());
        }
        int componentCount = compiledByRoot.size();
        double[] volume = new double[componentCount];
        double[] centroidX = new double[componentCount];
        double[] centroidY = new double[componentCount];
        double[] centroidZ = new double[componentCount];
        accumulateGeometry(
                blockGeometry,
                blockAtomOffset,
                atomCompiledComponent,
                volume,
                centroidX,
                centroidY,
                centroidZ
        );

        int maximumPorts = Math.multiplyExact(atomCount, ConservativeAirGeometry.Face.COUNT);
        byte[] portFace = new byte[maximumPorts];
        byte[] portBlockSlot = new byte[maximumPorts];
        int[] portComponent = new int[maximumPorts];
        int[] portAperture = new int[maximumPorts];
        int portCount = compileFacePorts(
                blockGeometry,
                blockAtomOffset,
                atomCompiledComponent,
                portFace,
                portBlockSlot,
                portComponent,
                portAperture
        );

        CompiledBrick brick = new CompiledBrick(
                blockAtomOffset,
                atomCompiledComponent,
                volume,
                centroidX,
                centroidY,
                centroidZ,
                Arrays.copyOf(portFace, portCount),
                Arrays.copyOf(portBlockSlot, portCount),
                Arrays.copyOf(portComponent, portCount),
                Arrays.copyOf(portAperture, portCount)
        );
        return new Compilation(Status.RESOLVED, UnsupportedReason.NONE, -1, Optional.of(brick));
    }

    public static int blockIndex(int x, int y, int z) {
        requireBlockCoordinate("x", x);
        requireBlockCoordinate("y", y);
        requireBlockCoordinate("z", z);
        return (y << 4) | (z << 2) | x;
    }

    private static void connectInteriorFaces(
            List<ConservativeAirGeometry.Resolution> geometry,
            int[] blockAtomOffset,
            int[] parent
    ) {
        for (int y = 0; y < BLOCKS_PER_AXIS; y++) {
            for (int z = 0; z < BLOCKS_PER_AXIS; z++) {
                for (int x = 0; x < BLOCKS_PER_AXIS; x++) {
                    int block = blockIndex(x, y, z);
                    if (x + 1 < BLOCKS_PER_AXIS) {
                        connectBlocks(
                                geometry, blockAtomOffset, parent,
                                block, blockIndex(x + 1, y, z),
                                ConservativeAirGeometry.Face.POSITIVE_X,
                                ConservativeAirGeometry.Face.NEGATIVE_X
                        );
                    }
                    if (y + 1 < BLOCKS_PER_AXIS) {
                        connectBlocks(
                                geometry, blockAtomOffset, parent,
                                block, blockIndex(x, y + 1, z),
                                ConservativeAirGeometry.Face.POSITIVE_Y,
                                ConservativeAirGeometry.Face.NEGATIVE_Y
                        );
                    }
                    if (z + 1 < BLOCKS_PER_AXIS) {
                        connectBlocks(
                                geometry, blockAtomOffset, parent,
                                block, blockIndex(x, y, z + 1),
                                ConservativeAirGeometry.Face.POSITIVE_Z,
                                ConservativeAirGeometry.Face.NEGATIVE_Z
                        );
                    }
                }
            }
        }
    }

    private static void connectBlocks(
            List<ConservativeAirGeometry.Resolution> geometry,
            int[] blockAtomOffset,
            int[] parent,
            int blockA,
            int blockB,
            ConservativeAirGeometry.Face faceA,
            ConservativeAirGeometry.Face faceB
    ) {
        List<ConservativeAirGeometry.AirComponent> componentsA = geometry.get(blockA).components();
        List<ConservativeAirGeometry.AirComponent> componentsB = geometry.get(blockB).components();
        for (int regionA = 0; regionA < componentsA.size(); regionA++) {
            int maskA = componentsA.get(regionA).faceMask(faceA);
            if (maskA == 0) {
                continue;
            }
            for (int regionB = 0; regionB < componentsB.size(); regionB++) {
                if ((maskA & componentsB.get(regionB).faceMask(faceB)) != 0) {
                    union(
                            parent,
                            blockAtomOffset[blockA] + regionA,
                            blockAtomOffset[blockB] + regionB
                    );
                }
            }
        }
    }

    private static void accumulateGeometry(
            List<ConservativeAirGeometry.Resolution> geometry,
            int[] blockAtomOffset,
            int[] atomCompiledComponent,
            double[] volume,
            double[] centroidX,
            double[] centroidY,
            double[] centroidZ
    ) {
        double microcellVolume = 1.0D / ConservativeAirGeometry.MICROCELL_COUNT;
        for (int block = 0; block < BLOCK_COUNT; block++) {
            int blockX = block & 3;
            int blockZ = (block >>> 2) & 3;
            int blockY = (block >>> 4) & 3;
            List<ConservativeAirGeometry.AirComponent> components = geometry.get(block).components();
            for (int localRegion = 0; localRegion < components.size(); localRegion++) {
                int compiled = atomCompiledComponent[blockAtomOffset[block] + localRegion];
                long mask = components.get(localRegion).microcellMask();
                while (mask != 0L) {
                    int microcell = Long.numberOfTrailingZeros(mask);
                    mask &= mask - 1L;
                    int microX = microcell & 3;
                    int microZ = (microcell >>> 2) & 3;
                    int microY = (microcell >>> 4) & 3;
                    volume[compiled] += microcellVolume;
                    centroidX[compiled] +=
                            (blockX + (microX + 0.5D) / 4.0D) * microcellVolume;
                    centroidY[compiled] +=
                            (blockY + (microY + 0.5D) / 4.0D) * microcellVolume;
                    centroidZ[compiled] +=
                            (blockZ + (microZ + 0.5D) / 4.0D) * microcellVolume;
                }
            }
        }
        for (int component = 0; component < volume.length; component++) {
            centroidX[component] /= volume[component];
            centroidY[component] /= volume[component];
            centroidZ[component] /= volume[component];
        }
    }

    private static int compileFacePorts(
            List<ConservativeAirGeometry.Resolution> geometry,
            int[] blockAtomOffset,
            int[] atomCompiledComponent,
            byte[] portFace,
            byte[] portBlockSlot,
            int[] portComponent,
            int[] portAperture
    ) {
        int portCount = 0;
        for (int block = 0; block < BLOCK_COUNT; block++) {
            int x = block & 3;
            int z = (block >>> 2) & 3;
            int y = (block >>> 4) & 3;
            List<ConservativeAirGeometry.AirComponent> components = geometry.get(block).components();
            for (int localRegion = 0; localRegion < components.size(); localRegion++) {
                ConservativeAirGeometry.AirComponent component = components.get(localRegion);
                int compiledComponent = atomCompiledComponent[blockAtomOffset[block] + localRegion];
                if (x == 0) {
                    portCount = appendPort(component, ConservativeAirGeometry.Face.NEGATIVE_X,
                            faceSlot(z, y), compiledComponent, portCount,
                            portFace, portBlockSlot, portComponent, portAperture);
                }
                if (x == BLOCKS_PER_AXIS - 1) {
                    portCount = appendPort(component, ConservativeAirGeometry.Face.POSITIVE_X,
                            faceSlot(z, y), compiledComponent, portCount,
                            portFace, portBlockSlot, portComponent, portAperture);
                }
                if (y == 0) {
                    portCount = appendPort(component, ConservativeAirGeometry.Face.NEGATIVE_Y,
                            faceSlot(x, z), compiledComponent, portCount,
                            portFace, portBlockSlot, portComponent, portAperture);
                }
                if (y == BLOCKS_PER_AXIS - 1) {
                    portCount = appendPort(component, ConservativeAirGeometry.Face.POSITIVE_Y,
                            faceSlot(x, z), compiledComponent, portCount,
                            portFace, portBlockSlot, portComponent, portAperture);
                }
                if (z == 0) {
                    portCount = appendPort(component, ConservativeAirGeometry.Face.NEGATIVE_Z,
                            faceSlot(x, y), compiledComponent, portCount,
                            portFace, portBlockSlot, portComponent, portAperture);
                }
                if (z == BLOCKS_PER_AXIS - 1) {
                    portCount = appendPort(component, ConservativeAirGeometry.Face.POSITIVE_Z,
                            faceSlot(x, y), compiledComponent, portCount,
                            portFace, portBlockSlot, portComponent, portAperture);
                }
            }
        }
        return portCount;
    }

    private static int appendPort(
            ConservativeAirGeometry.AirComponent component,
            ConservativeAirGeometry.Face face,
            int blockSlot,
            int compiledComponent,
            int portCount,
            byte[] portFace,
            byte[] portBlockSlot,
            int[] portComponent,
            int[] portAperture
    ) {
        int aperture = component.faceMask(face);
        if (aperture == 0) {
            return portCount;
        }
        portFace[portCount] = (byte) face.ordinal();
        portBlockSlot[portCount] = (byte) blockSlot;
        portComponent[portCount] = compiledComponent;
        portAperture[portCount] = aperture;
        return portCount + 1;
    }

    private static Compilation unsupported(UnsupportedReason reason, int blockIndex) {
        return new Compilation(Status.CONSERVATIVE_UNSUPPORTED, reason, blockIndex, Optional.empty());
    }

    private static int faceSlot(int u, int v) {
        return (v << 2) | u;
    }

    private static int find(int[] parent, int value) {
        int root = value;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[value] != value) {
            int next = parent[value];
            parent[value] = root;
            value = next;
        }
        return root;
    }

    private static void union(int[] parent, int a, int b) {
        int rootA = find(parent, a);
        int rootB = find(parent, b);
        if (rootA == rootB) {
            return;
        }
        if (rootA < rootB) {
            parent[rootB] = rootA;
        } else {
            parent[rootA] = rootB;
        }
    }

    private static void requireBlockIndex(int blockIndex) {
        requireIndex("blockIndex", blockIndex, BLOCK_COUNT);
    }

    private static void requireBlockCoordinate(String name, int value) {
        if (value < 0 || value >= BLOCKS_PER_AXIS) {
            throw new IllegalArgumentException(name + " must be within [0, 3]");
        }
    }

    private static void requireIndex(String name, int value, int length) {
        if (value < 0 || value >= length) {
            throw new IllegalArgumentException(name + " is out of bounds");
        }
    }
}
