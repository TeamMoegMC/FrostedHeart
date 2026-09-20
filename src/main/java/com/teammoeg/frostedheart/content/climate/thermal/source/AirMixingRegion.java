/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;

/** Reused source positions for the direct Air faces owned by one Brick. */
public final class AirMixingRegion {
    public static final int RADIUS = 4;
    public static final int MULTIPLIER = 4;
    private final LongArrayList ports = new LongArrayList();
    private int minX, minY, minZ;

    public void reset(int brickX, int brickY, int brickZ) {
        ports.clear();
        minX = brickX - RADIUS;
        minY = brickY - RADIUS;
        minZ = brickZ - RADIUS;
    }

    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return minX + 2 * RADIUS + 3; }
    public int maxY() { return minY + 2 * RADIUS + 3; }
    public int maxZ() { return minZ + 2 * RADIUS + 3; }

    public void include(int x, int y, int z) {
        if (x >= minX && x <= maxX() && y >= minY && y <= maxY() && z >= minZ && z <= maxZ()) {
            ports.add(BlockPos.asLong(x, y, z));
        }
    }

    /** Piecewise volume coefficient for the continuous field, with the same radius and union. */
    public double volumeScale(double x, double y, double z) {
        for (int i = 0; i < ports.size(); i++) {
            long port = ports.getLong(i);
            double dx = x - BlockPos.getX(port) - 0.5;
            double dy = y - BlockPos.getY(port) - 0.5;
            double dz = z - BlockPos.getZ(port) - 0.5;
            if (dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS) return MULTIPLIER;
        }
        return 1;
    }

    /** Unit face on the positive side of the given block. Overlap is a union. */
    public double faceArea(int axis, int x, int y, int z) {
        for (int i = 0; i < ports.size(); i++) {
            long port = ports.getLong(i);
            double dx = x - BlockPos.getX(port) + (axis == 0 ? .5 : 0);
            double dy = y - BlockPos.getY(port) + (axis == 1 ? .5 : 0);
            double dz = z - BlockPos.getZ(port) + (axis == 2 ? .5 : 0);
            if (dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS) return MULTIPLIER;
        }
        return 1;
    }

    /** Preserve the full-Air fast path; only intersecting faces need 16 tests. */
    public double brickFaceArea(int axis, int brickX, int brickY, int brickZ) {
        if (ports.isEmpty()) return 16;
        double area = 0;
        for (int i = 0; i < 16; i++) {
            int block = BlockBrickLayout.faceBlock(axis, 3, i);
            area += faceArea(axis, brickX + (block & 3), brickY + (block >>> 4), brickZ + (block >>> 2 & 3));
        }
        return area;
    }
}
