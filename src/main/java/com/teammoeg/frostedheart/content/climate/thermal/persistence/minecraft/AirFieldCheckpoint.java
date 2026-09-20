/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.LocalAirShape;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** Immutable section-owned field values. Runtime addresses and solver arrays are never persisted. */
public final class AirFieldCheckpoint {
    private final int minX, minY, minZ;
    private final Component[] components;
    private final LocalAirShape[] shapes;
    private final double[] amplitudesC;
    private final long brickMask;

    private AirFieldCheckpoint(int minX, int minY, int minZ, Component[] components,
            LocalAirShape[] shapes, double[] amplitudesC) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.components = components;
        this.shapes = shapes;
        this.amplitudesC = amplitudesC;
        long mask = 0;
        for (Component component : components) mask |= 1L << component.brick;
        brickMask = mask;
    }

    public long brickMask() { return brickMask; }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int shapeCount() { return shapes.length; }
    public LocalAirShape shape(int index) { return shapes[index]; }
    public double amplitudeC(int index) { return amplitudesC[index]; }

    public static AirFieldCheckpoint capture(AirFieldLayout layout, double[] coefficientsC,
            double referenceC, PagePublication page, double naturalC) {
        ArrayList<Component> components = new ArrayList<>();
        ArrayList<LocalAirShape> shapes = new ArrayList<>();
        it.unimi.dsi.fastutil.doubles.DoubleArrayList amplitudes = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        IdentityHashMap<LocalAirShape, Integer> shapeIndices = new IdentityHashMap<>();
        int minX = 0, minY = 0, minZ = 0;
        for (int brick = 0; brick < 64; brick++) {
            var payload = page.brick(brick);
            for (int node = 0; node < payload.airNodeCount(); node++) {
                var component = layout.componentForSlot(payload.firstSlot() + node);
                if (component == null || component.arenaGeneration != payload.arenaGeneration()) continue;
                double lower = referenceC + component.minimumOffsetC(coefficientsC);
                double upper = referenceC + component.maximumOffsetC(coefficientsC);
                if (Math.max(Math.abs(lower - naturalC), Math.abs(upper - naturalC)) <= 0.0625) continue;
                minX = component.minX & ~15;
                minY = component.minY & ~15;
                minZ = component.minZ & ~15;
                double[] coarseC = new double[8];
                for (int corner = 0; corner < 8; corner++) coarseC[corner] = referenceC + coefficientsC[component.coefficientIndex(corner)];
                int[] localModes = new int[component.basisCount() - 8];
                for (int mode = 0; mode < localModes.length; mode++) {
                    int coefficient = component.coefficientIndex(mode + 8);
                    LocalAirShape shape = layout.localShape(coefficient - layout.coarseCount());
                    Integer index = shapeIndices.get(shape);
                    if (index == null) {
                        index = shapes.size();
                        shapeIndices.put(shape, index);
                        shapes.add(shape);
                        amplitudes.add(coefficientsC[coefficient]);
                    }
                    localModes[mode] = index;
                }
                components.add(new Component(brick, component.airBlocks, coarseC, localModes));
            }
        }
        return components.isEmpty() ? null : new AirFieldCheckpoint(minX, minY, minZ,
                components.toArray(Component[]::new), shapes.toArray(LocalAirShape[]::new), amplitudes.toDoubleArray());
    }

    public double sampleC(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        int lx = bx - minX, ly = by - minY, lz = bz - minZ;
        if (lx < 0 || lx >= 16 || ly < 0 || ly >= 16 || lz < 0 || lz >= 16) return Double.NaN;
        int brick = lx >>> 2 | (lz >>> 2) << 2 | (ly >>> 2) << 4;
        int block = (lx & 3) | (lz & 3) << 2 | (ly & 3) << 4;
        for (Component component : components) {
            if (component.brick != brick || (component.blocks & 1L << block) == 0) continue;
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
            long[] values = new long[8];
            for (int index = 0; index < 8; index++) values[index] = Double.doubleToLongBits(component.coarseC[index]);
            cell.putLongArray("coarse", values);
            cell.putIntArray("modes", component.modes);
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
                    corners, values, new ThermalPageHandle[0], new long[0]).withBranch(shape.getLong("branch"));
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
            components[index] = new Component(brick, blocks, coarse, modes);
        }
        return new AirFieldCheckpoint(BlockPos.getX(origin), BlockPos.getY(origin), BlockPos.getZ(origin), components, shapes, amplitudes);
    }

    /** Section merge preserves unsampled Bricks with their full basis, rather than scalar means. */
    public static AirFieldCheckpoint merge(AirFieldCheckpoint first, double firstNatural, double firstFactor,
            AirFieldCheckpoint second, double secondNatural, double secondFactor, long secondBricks) {
        if (first == null && second == null) return null;
        AirFieldCheckpoint origin = second == null ? first : second;
        ArrayList<Component> components = new ArrayList<>();
        ArrayList<LocalAirShape> shapes = new ArrayList<>();
        it.unimi.dsi.fastutil.doubles.DoubleArrayList amplitudes = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        append(first, ~secondBricks, firstNatural, firstFactor, components, shapes, amplitudes);
        append(second, secondBricks, secondNatural, secondFactor, components, shapes, amplitudes);
        return components.isEmpty() ? null : new AirFieldCheckpoint(origin.minX, origin.minY, origin.minZ,
                components.toArray(Component[]::new), shapes.toArray(LocalAirShape[]::new), amplitudes.toDoubleArray());
    }

    private static void append(AirFieldCheckpoint source, long mask, double natural, double factor,
            List<Component> components, List<LocalAirShape> shapes, it.unimi.dsi.fastutil.doubles.DoubleArrayList amplitudes) {
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
            components.add(new Component(component.brick, component.blocks, coarse, modes));
        }
    }

    private record Component(int brick, long blocks, double[] coarseC, int[] modes) {}
}
