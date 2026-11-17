/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

/**
 * Exact Q1 corner identities from its eight incident unit Air cells. A shared
 * corner union never leaves this 2x2x2 neighbourhood, so no region-wide union-find
 * is needed. Octant bits are X=1, Z=2, Y=4, matching the existing Q1 basis.
 */
public final class AirCornerTopology {
    public static final int MAXIMUM_GROUPS = 4;
    private static final byte[] COUNTS = new byte[256];
    private static final int[] COMPONENTS = new int[256], GROUPS = new int[256];

    static {
        for (int mask = 0; mask < 256; mask++) {
            int remaining = mask, count = 0, assignments = 0xFFFFFF, components = 0;
            while (remaining != 0) {
                int connected = Integer.lowestOneBit(remaining), frontier = connected;
                while (frontier != 0) {
                    int voxel = Integer.numberOfTrailingZeros(frontier);
                    frontier &= frontier - 1;
                    int neighbours = (1 << (voxel ^ 1) | 1 << (voxel ^ 2) | 1 << (voxel ^ 4)) & remaining & ~connected;
                    connected |= neighbours; frontier |= neighbours;
                }
                components |= connected << (8 * count);
                int members = connected;
                while (members != 0) {
                    int octant = Integer.numberOfTrailingZeros(members); members &= members - 1;
                    assignments = assignments & ~(7 << (octant * 3)) | count << (octant * 3);
                }
                count++; remaining &= ~connected;
            }
            COUNTS[mask] = (byte) count; COMPONENTS[mask] = components; GROUPS[mask] = assignments;
        }
    }

    private AirCornerTopology() { }

    public static int count(int occupiedOctants) { return COUNTS[occupiedOctants & 255]; }

    public static int component(int occupiedOctants, int group) {
        return COMPONENTS[occupiedOctants & 255] >>> (group * 8) & 255;
    }

    /** -1 means this incident voxel does not own an Air corner here. */
    public static int group(int occupiedOctants, int octant) {
        int value = GROUPS[occupiedOctants & 255] >>> (octant * 3) & 7;
        return value == 7 ? -1 : value;
    }
}
