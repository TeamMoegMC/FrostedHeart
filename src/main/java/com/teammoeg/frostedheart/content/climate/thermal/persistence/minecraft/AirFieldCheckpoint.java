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
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.LocalAirShape;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** Immutable section-owned field values. Runtime addresses and solver arrays are never persisted. */
public final class AirFieldCheckpoint {
    @FunctionalInterface
    public interface PointSampler {
        double temperatureC(double x, double y, double z);
    }
    private final int minX, minY, minZ;
    private final Component[] components;
    private final LocalAirShape[] shapes;
    private final double[] amplitudesC;
    private final long brickMask;
    private final int[] firstComponentByBrick, nextComponent;

    private AirFieldCheckpoint(int minX, int minY, int minZ, Component[] components,
            LocalAirShape[] shapes, double[] amplitudesC) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.components = components;
        this.shapes = shapes;
        this.amplitudesC = amplitudesC;
        firstComponentByBrick = new int[64];
        nextComponent = new int[components.length];
        java.util.Arrays.fill(firstComponentByBrick, -1);
        long mask = 0;
        for (int index = components.length - 1; index >= 0; index--) {
            int brick = components[index].brick;
            mask |= 1L << brick;
            nextComponent[index] = firstComponentByBrick[brick];
            firstComponentByBrick[brick] = index;
        }
        brickMask = mask;
    }

    public long brickMask() { return brickMask; }

    public void recordBacking(AirBackingMeter meter) {
        meter.bytes(64L + 64L * components.length);
        meter.array(components); meter.array(shapes); meter.array(amplitudesC);
        meter.array(firstComponentByBrick); meter.array(nextComponent);
        for (Component component : components) {
            meter.array(component.coarseC); meter.array(component.modes);
            meter.array(component.unitCorners); meter.array(component.nodalC);
        }
        for (LocalAirShape shape : shapes) shape.recordBacking(meter);
    }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int shapeCount() { return shapes.length; }
    public LocalAirShape shape(int index) { return shapes[index]; }
    public double amplitudeC(int index) { return amplitudesC[index]; }

    /** Collect only this Page's values, using each component's original coefficient addresses. */
    public static final class Capture {
        private final double naturalC;
        private final ArrayList<Component> components = new ArrayList<>();
        private int minX, minY, minZ;
        private long sampleTick = -1;

        public Capture(double naturalC) { this.naturalC = naturalC; }
        public Capture atTick(long tick) { sampleTick = tick; return this; }

        /** Minimum fields are captured locally; no whole-region coefficient expansion is needed. */
        public void add(AirFieldLayout layout, PointSampler sample, int brick, int slot, int generation) {
            var component = layout.componentForSlot(slot);
            if (component == null || component.arenaGeneration != generation) return;
            double[] values = new double[8];
            double residual = 0;
            for (int corner = 0; corner < 8; corner++) {
                values[corner] = sample.temperatureC(component.minX + ((corner & 1) == 0 ? 0 : 4),
                        component.minY + ((corner & 4) == 0 ? 0 : 4),
                        component.minZ + ((corner & 2) == 0 ? 0 : 4));
                residual = Math.max(residual, Math.abs(values[corner] - naturalC));
            }
            if (residual <= .0625) return;
            minX = component.minX & ~15; minY = component.minY & ~15; minZ = component.minZ & ~15;
            components.add(new Component(brick, component.airBlocks, values, new int[0], null, null, sampleTick));
        }

        public void add(AirFieldLayout layout, double[] coefficientsC, double referenceC,
                int brick, int slot, int generation) {
            var component = layout.componentForSlot(slot);
            if (component == null || component.arenaGeneration != generation) return;
            double lower = referenceC + component.minimumOffsetC(coefficientsC);
            double upper = referenceC + component.maximumOffsetC(coefficientsC);
            if (Math.max(Math.abs(lower - naturalC), Math.abs(upper - naturalC)) <= 0.0625) return;
            minX = component.minX & ~15;
            minY = component.minY & ~15;
            minZ = component.minZ & ~15;
            if (component.isUnitGrid()) {
                double[] coarseC = new double[8];
                java.util.Arrays.fill(coarseC, referenceC);
                double[] nodalC = new double[component.basisCount()];
                for (int basis = 0; basis < nodalC.length; basis++)
                    nodalC[basis] = referenceC + coefficientsC[component.coefficientIndex(basis)];
                components.add(new Component(brick, component.airBlocks, coarseC,
                        new int[0], component.unitCornersCopy(), nodalC, sampleTick));
                return;
            }
            double[] coarseC = new double[8];
            for (int corner = 0; corner < 8; corner++)
                coarseC[corner] = referenceC + coefficientsC[component.coefficientIndex(corner)];
            components.add(new Component(brick, component.airBlocks, coarseC, new int[0], null, null, sampleTick));
        }

        public AirFieldCheckpoint finish() {
            return components.isEmpty() ? null : new AirFieldCheckpoint(minX, minY, minZ,
                    components.toArray(Component[]::new), new LocalAirShape[0], new double[0]);
        }
    }

    public double sampleC(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        int lx = bx - minX, ly = by - minY, lz = bz - minZ;
        if (lx < 0 || lx >= 16 || ly < 0 || ly >= 16 || lz < 0 || lz >= 16) return Double.NaN;
        int brick = lx >>> 2 | (lz >>> 2) << 2 | (ly >>> 2) << 4;
        int block = (lx & 3) | (lz & 3) << 2 | (ly & 3) << 4;
        for (int index = firstComponentByBrick[brick]; index >= 0; index = nextComponent[index]) {
            Component component = components[index];
            if ((component.blocks & 1L << block) == 0) continue;
            if (component.unitCorners != null) {
                double dx = x - bx, dy = y - by, dz = z - bz, temperatureC = 0;
                for (int corner = 0; corner < 8; corner++) temperatureC += component.nodalC[component.unitCorners[block * 8 + corner]]
                        * ((corner & 1) == 0 ? 1 - dx : dx)
                        * ((corner & 4) == 0 ? 1 - dy : dy)
                        * ((corner & 2) == 0 ? 1 - dz : dz);
                return temperatureC;
            }
            double dx = (x - (bx & ~3)) * 0.25;
            double dy = (y - (by & ~3)) * 0.25;
            double dz = (z - (bz & ~3)) * 0.25;
            double temperatureC = 0;
            for (int corner = 0; corner < 8; corner++) temperatureC += component.coarseC[corner]
                    * ((corner & 1) == 0 ? 1 - dx : dx)
                    * ((corner & 4) == 0 ? 1 - dy : dy)
                    * ((corner & 2) == 0 ? 1 - dz : dz);
            for (int mode : component.modes) temperatureC += amplitudesC[mode] * shapes[mode].value(bx, by, bz, x, y, z);
            return temperatureC;
        }
        return Double.NaN;
    }

    public double blockCenterC(int brick, int block) {
        int x = minX + ((brick & 3) << 2) + (block & 3);
        int y = minY + ((brick >>> 4) << 2) + (block >>> 4);
        int z = minZ + ((brick >>> 2 & 3) << 2) + (block >>> 2 & 3);
        return sampleC(x + 0.5, y + 0.5, z + 0.5);
    }
    public long sampleTickAt(double x, double y, double z, long fallback) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        int lx = bx - minX, ly = by - minY, lz = bz - minZ;
        if (lx < 0 || lx >= 16 || ly < 0 || ly >= 16 || lz < 0 || lz >= 16) return fallback;
        int brick = lx >>> 2 | (lz >>> 2) << 2 | (ly >>> 2) << 4;
        int block = (lx & 3) | (lz & 3) << 2 | (ly & 3) << 4;
        for (int index = firstComponentByBrick[brick]; index >= 0; index = nextComponent[index]) {
            var component = components[index];
            if ((component.blocks & 1L << block) != 0) return component.sampleTick < 0 ? fallback : component.sampleTick;
        }
        return fallback;
    }
    public double sampleC(double x, double y, double z, double natural, long tick, double rate, long fallbackTick) {
        double saved = sampleC(x, y, z);
        return Double.isFinite(saved) ? com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling.temperature(
                saved, natural, com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling.factor(
                        sampleTickAt(x, y, z, fallbackTick), tick, rate)) : Double.NaN;
    }
    public double blockCenterC(int brick, int block, double natural, long tick, double rate, long fallbackTick) {
        double x = minX + ((brick & 3) << 2) + (block & 3) + .5;
        double y = minY + ((brick >>> 4) << 2) + (block >>> 4) + .5;
        double z = minZ + ((brick >>> 2 & 3) << 2) + (block >>> 2 & 3) + .5;
        return sampleC(x, y, z, natural, tick, rate, fallbackTick);
    }
    public double meanC(int brick, double natural, long tick, double rate, long fallbackTick) {
        double sum = 0; int count = 0;
        for (int block = 0; block < 64; block++) {
            double value = blockCenterC(brick, block, natural, tick, rate, fallbackTick);
            if (Double.isFinite(value)) { sum += value; count++; }
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    public double meanC(int brick) {
        double sum = 0;
        int count = 0;
        for (int block = 0; block < 64; block++) {
            double value = blockCenterC(brick, block);
            if (Double.isFinite(value)) { sum += value; count++; }
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("origin", BlockPos.asLong(minX, minY, minZ));
        ListTag cells = new ListTag();
        for (Component component : components) {
            CompoundTag cell = new CompoundTag();
            cell.putByte("brick", (byte) component.brick);
            cell.putLong("blocks", component.blocks);
            if (component.sampleTick >= 0) cell.putLong("sample_tick", component.sampleTick);
            long[] values = new long[8];
            for (int index = 0; index < 8; index++) values[index] = Double.doubleToLongBits(component.coarseC[index]);
            cell.putLongArray("coarse", values);
            cell.putIntArray("modes", component.modes);
            if (component.unitCorners != null) {
                cell.putIntArray("unitCorners", component.unitCorners);
                long[] nodal = new long[component.nodalC.length];
                for (int i = 0; i < nodal.length; i++) nodal[i] = Double.doubleToLongBits(component.nodalC[i]);
                cell.putLongArray("unitValues", nodal);
            }
            cells.add(cell);
        }
        tag.put("components", cells);
        ListTag shapeTags = new ListTag();
        for (int index = 0; index < shapes.length; index++) {
            LocalAirShape shape = shapes[index].sectionSlice(minX, minY, minZ);
            CompoundTag shapeTag = new CompoundTag();
            shapeTag.putLong("origin", BlockPos.asLong(shape.minX, shape.minY, shape.minZ));
            shapeTag.putIntArray("size", new int[]{shape.sizeX, shape.sizeY, shape.sizeZ});
            shapeTag.putLong("source", shape.sourceBlock);
            shapeTag.putByte("face", shape.sourceFace);
            shapeTag.putInt("scale", shape.scale);
            shapeTag.putLong("branch", shape.branch);
            shapeTag.putDouble("amplitude", amplitudesC[index]);
            long[] corners = new long[(shape.cornerCount() + 3) / 4];
            for (int corner = 0; corner < shape.cornerCount(); corner++) corners[corner >>> 2] |=
                    (long) Short.toUnsignedInt(shape.cornerIndex(corner)) << ((corner & 3) * 16);
            int[] values = new int[shape.vertexCount()];
            for (int vertex = 0; vertex < values.length; vertex++) values[vertex] = Float.floatToIntBits(shape.vertexValue(vertex));
            shapeTag.putLongArray("corners", corners);
            shapeTag.putIntArray("values", values);
            shapeTags.add(shapeTag);
        }
        tag.put("shapes", shapeTags);
        return tag;
    }

    public static AirFieldCheckpoint decode(CompoundTag tag) {
        long origin = tag.getLong("origin");
        ListTag shapeTags = tag.getList("shapes", Tag.TAG_COMPOUND);
        LocalAirShape[] shapes = new LocalAirShape[shapeTags.size()];
        double[] amplitudes = new double[shapes.length];
        for (int index = 0; index < shapes.length; index++) {
            CompoundTag shape = shapeTags.getCompound(index);
            int[] size = shape.getIntArray("size");
            if (size.length != 3 || size[0] <= 0 || size[0] > 16 || size[1] <= 0 || size[1] > 16 || size[2] <= 0 || size[2] > 16) return null;
            int cornerCount = size[0] * size[1] * size[2] * 8;
            long[] packed = shape.getLongArray("corners");
            int[] bits = shape.getIntArray("values");
            if (packed.length != (cornerCount + 3) / 4 || bits.length >= 65535) return null;
            short[] corners = new short[cornerCount];
            for (int corner = 0; corner < corners.length; corner++) {
                corners[corner] = (short) (packed[corner >>> 2] >>> ((corner & 3) * 16));
                if (corners[corner] != (short) -1 && Short.toUnsignedInt(corners[corner]) >= bits.length) return null;
            }
            float[] values = new float[bits.length];
            for (int vertex = 0; vertex < values.length; vertex++) {
                values[vertex] = Float.intBitsToFloat(bits[vertex]);
                if (!Float.isFinite(values[vertex])) return null;
            }
            long position = shape.getLong("origin");
            shapes[index] = new LocalAirShape(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position),
                    size[0], size[1], size[2], shape.getLong("source"), shape.getByte("face"), shape.getInt("scale"),
                    corners, values, shape.getLong("branch"));
            if (shapes[index].sourceFace < 0 || shapes[index].sourceFace >= 6) return null;
            amplitudes[index] = shape.getDouble("amplitude");
            if (!Double.isFinite(amplitudes[index])) return null;
        }
        ListTag cells = tag.getList("components", Tag.TAG_COMPOUND);
        Component[] components = new Component[cells.size()];
        long[] seenBlocks = new long[64];
        for (int index = 0; index < components.length; index++) {
            CompoundTag cell = cells.getCompound(index);
            int brick = Byte.toUnsignedInt(cell.getByte("brick"));
            long blocks = cell.getLong("blocks");
            long[] values = cell.getLongArray("coarse");
            int[] modes = cell.getIntArray("modes");
            if (brick >= 64 || blocks == 0 || values.length != 8 || (seenBlocks[brick] & blocks) != 0) return null;
            seenBlocks[brick] |= blocks;
            double[] coarse = new double[8];
            for (int corner = 0; corner < 8; corner++) {
                coarse[corner] = Double.longBitsToDouble(values[corner]);
                if (!Double.isFinite(coarse[corner])) return null;
            }
            for (int mode : modes) if (mode < 0 || mode >= shapes.length) return null;
            if (cell.contains("unitCorners")) {
                int[] corners = cell.getIntArray("unitCorners");
                long[] nodalBits = cell.getLongArray("unitValues");
                if (corners.length != 512 || nodalBits.length == 0 || nodalBits.length > 512) return null;
                double[] nodal = new double[nodalBits.length];
                for (int i = 0; i < nodal.length; i++) {
                    nodal[i] = Double.longBitsToDouble(nodalBits[i]);
                    if (!Double.isFinite(nodal[i])) return null;
                }
                long membership = blocks;
                while (membership != 0) {
                    int block = Long.numberOfTrailingZeros(membership); membership &= membership - 1;
                    for (int corner = 0; corner < 8; corner++) {
                        int node = corners[block * 8 + corner];
                        if (node < 0 || node >= nodal.length) return null;
                    }
                }
                components[index] = new Component(brick, blocks, coarse, modes, corners, nodal,
                        cell.contains("sample_tick") ? cell.getLong("sample_tick") : -1);
            } else components[index] = new Component(brick, blocks, coarse, modes, null, null,
                    cell.contains("sample_tick") ? cell.getLong("sample_tick") : -1);
        }
        return new AirFieldCheckpoint(BlockPos.getX(origin), BlockPos.getY(origin), BlockPos.getZ(origin), components, shapes, amplitudes);
    }

    /** Preserve individual accepted clocks; merging sections does not advance any temperature. */
    public static AirFieldCheckpoint mergeDated(AirFieldCheckpoint first, long firstTick,
            AirFieldCheckpoint second, long secondTick, long secondBricks) {
        if (first == null && second == null) return null;
        AirFieldCheckpoint origin = second == null ? first : second;
        var components = new ArrayList<Component>();
        var shapes = new ArrayList<LocalAirShape>();
        var amplitudes = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        append(first, ~secondBricks, 0, 1, components, shapes, amplitudes, firstTick);
        append(second, secondBricks, 0, 1, components, shapes, amplitudes, secondTick);
        return components.isEmpty() ? null : new AirFieldCheckpoint(origin.minX, origin.minY, origin.minZ,
                components.toArray(Component[]::new), shapes.toArray(LocalAirShape[]::new), amplitudes.toDoubleArray());
    }

    private static void append(AirFieldCheckpoint source, long mask, double natural, double factor,
            List<Component> components, List<LocalAirShape> shapes,
            it.unimi.dsi.fastutil.doubles.DoubleArrayList amplitudes, long fallbackTick) {
        if (source == null || (source.brickMask & mask) == 0) return;
        int[] remap = new int[source.shapes.length];
        java.util.Arrays.fill(remap, -1);
        for (Component component : source.components) {
            if ((mask & 1L << component.brick) == 0) continue;
            double[] coarse = component.coarseC.clone();
            for (int corner = 0; corner < 8; corner++) coarse[corner] = natural + (coarse[corner] - natural) * factor;
            int[] modes = component.modes.clone();
            for (int index = 0; index < modes.length; index++) {
                int old = modes[index];
                if (remap[old] < 0) {
                    remap[old] = shapes.size();
                    shapes.add(source.shapes[old]);
                    amplitudes.add(source.amplitudesC[old] * factor);
                }
                modes[index] = remap[old];
            }
            double[] nodal = component.nodalC == null ? null : component.nodalC.clone();
            if (nodal != null) for (int i = 0; i < nodal.length; i++) nodal[i] = natural + (nodal[i] - natural) * factor;
            components.add(new Component(component.brick, component.blocks, coarse, modes, component.unitCorners, nodal,
                    factor == 1 ? component.sampleTick < 0 ? fallbackTick : component.sampleTick : -1));
        }
    }

    private record Component(int brick, long blocks, double[] coarseC, int[] modes, int[] unitCorners, double[] nodalC, long sampleTick) { }
}
