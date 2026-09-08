/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/** Immutable whole-block membership. Nodes are transport, material, then phase. */
public final class BlockBrickLayout {
    private final byte[] blockToNode;
    private final long[] nodeBlockMasks;
    private final int transportNodeCount;
    private static final long[] LOW = {0xaaaaaaaaaaaaaaaaL, 0xffff0000ffff0000L, 0xf0f0f0f0f0f0f0f0L};
    private static final long[] HIGH = {0xccccccccccccccccL, 0xffffffff00000000L, 0xff00ff00ff00ff00L};
    public BlockBrickLayout(byte[] blockToNode, long[] nodeBlockMasks, int transportNodeCount) {
        this.blockToNode = blockToNode;
        this.nodeBlockMasks = nodeBlockMasks;
        this.transportNodeCount = transportNodeCount;
    }
    public int nodeAt(int block) { int node = Byte.toUnsignedInt(blockToNode[block]); return node == 255 ? -1 : node; }
    public long nodeBlockMask(int node) { return nodeBlockMasks[node]; }
    public int transportNodeCount() { return transportNodeCount; }
    public int transportAt(int block) { int n = nodeAt(block); return n < transportNodeCount ? n : -1; }
    public double center(int node, int axis) {
        long mask = nodeBlockMasks[node];
        return 0.5 + (Long.bitCount(mask & LOW[axis]) + 2.0 * Long.bitCount(mask & HIGH[axis])) / Long.bitCount(mask);
    }
    public static int pageBlock(int brick, int block) {
        return ((brick & 3) * 4 + (block & 3))
                | ((brick >>> 2 & 3) * 4 + (block >>> 2 & 3)) << 4
                | ((brick >>> 4 & 3) * 4 + (block >>> 4 & 3)) << 8;
    }
    public static int faceBlock(int axis, int side, int index) {
        int a = index & 3, b = index >>> 2;
        return switch (axis) {
            case 0 -> side | a << 2 | b << 4;
            case 1 -> a | b << 2 | side << 4;
            case 2 -> a | side << 2 | b << 4;
            default -> throw new IllegalArgumentException("axis");
        };
    }
}
