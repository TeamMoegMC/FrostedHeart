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
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/**
 * Whole-block membership: Air nodes precede one node per material block.
 * Block indexes use x | z << 2 | y << 4; unsigned byte 255 means no node.
 * Material membership does not imply an exposed face. Route validity can be retired separately
 * when geometry changes, without rewriting this layout's membership arrays.
 */
public final class BlockBrickLayout {
    private final byte[] blockToNode;
    private final long[] nodeBlockMasks;
    private final int airNodeCount;
    private final long materialNodeMask;
    private final long routedAirMask;
    private final long[] routedAirBlocks;
    private final AirRouteValidity[] routeValidity;
    private final long airContactBlocks;
    private static final long[] LOW = {
        0xaaaaaaaaaaaaaaaaL, 0xffff0000ffff0000L, 0xf0f0f0f0f0f0f0f0L
    };
    private static final long[] HIGH = {
        0xccccccccccccccccL, 0xffffffff00000000L, 0xff00ff00ff00ff00L
    };

    public BlockBrickLayout(
            byte[] blockToNode, long[] nodeBlockMasks, int airNodeCount, long materialNodeMask) {
        this(blockToNode, nodeBlockMasks, airNodeCount, materialNodeMask, 0, null, null, 0);
    }

    private BlockBrickLayout(
            byte[] blockToNode,
            long[] nodeBlockMasks,
            int airNodeCount,
            long materialNodeMask,
            long routedAirMask,
            long[] routedAirBlocks,
            AirRouteValidity[] routeValidity,
            long airContactBlocks) {
        this.blockToNode = blockToNode;
        this.nodeBlockMasks = nodeBlockMasks;
        this.airNodeCount = airNodeCount;
        this.materialNodeMask = materialNodeMask;
        this.routedAirMask = routedAirMask;
        this.routedAirBlocks = routedAirBlocks;
        this.routeValidity = routeValidity;
        this.airContactBlocks = airContactBlocks;
    }

    public BlockBrickLayout withAirRoutes(long mask, long[] blocks, AirRouteValidity[] validity) {
        if (mask == 0 && routedAirMask == 0) return this;
        return new BlockBrickLayout(
                blockToNode,
                nodeBlockMasks,
                airNodeCount,
                materialNodeMask,
                mask,
                blocks,
                validity,
                airContactBlocks);
    }

    public long airContactBlocks() {
        return airContactBlocks;
    }

    public void recordBacking(AirBackingMeter meter) {
        if (!meter.objectOnce(this, meter.summaries() ? ownedBackingBytes() : 64)) return;
        if (!meter.summaries()) {
            meter.array(blockToNode); meter.array(nodeBlockMasks);
            meter.array(routedAirBlocks); meter.array(routeValidity);
        }
        if (routeValidity != null) for (var validity : routeValidity)
            if (validity != null) validity.recordBacking(meter);
    }

    /** Route witnesses belong to the routing component, rather than every Brick referring to it. */
    public long ownedBackingBytes() {
        return 64L + AirBackingMeter.shallowArrayBytes(blockToNode) + AirBackingMeter.shallowArrayBytes(nodeBlockMasks)
                + AirBackingMeter.shallowArrayBytes(routedAirBlocks) + AirBackingMeter.shallowArrayBytes(routeValidity);
    }

    public BlockBrickLayout withAirContactBlocks(long blocks) {
        return blocks == airContactBlocks
                ? this
                : new BlockBrickLayout(
                        blockToNode,
                        nodeBlockMasks,
                        airNodeCount,
                        materialNodeMask,
                        routedAirMask,
                        routedAirBlocks,
                        routeValidity,
                        blocks);
    }

    public boolean hasAirRoute(int block) {
        return (routedAirMask & 1L << block) != 0 && routeValidity[block].valid();
    }

    public boolean hasCommittedAirRoute(int block) {
        return (routedAirMask & 1L << block) != 0 && routeValidity[block].activeForSolve();
    }

    public long routedAirBlock(int block) {
        return routedAirBlocks[block];
    }

    public int nodeAt(int block) {
        int node = Byte.toUnsignedInt(blockToNode[block]);
        return node == 255 ? -1 : node;
    }

    public long nodeBlockMask(int node) {
        return nodeBlockMasks[node];
    }

    public int airNodeCount() {
        return airNodeCount;
    }

    public long materialNodeMask() {
        return materialNodeMask;
    }

    public int airNodeAt(int block) {
        int n = nodeAt(block);
        return n < airNodeCount ? n : -1;
    }

    public double center(int node, int axis) {
        long mask = nodeBlockMasks[node];
        return 0.5
                + (Long.bitCount(mask & LOW[axis]) + 2.0 * Long.bitCount(mask & HIGH[axis]))
                        / Long.bitCount(mask);
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
