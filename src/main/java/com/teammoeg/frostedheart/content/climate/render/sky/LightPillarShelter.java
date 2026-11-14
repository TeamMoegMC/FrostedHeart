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

package com.teammoeg.frostedheart.content.climate.render.sky;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Locale;

/**
 * The shelter of the lights in the slots of a LightPillarSelection, read from the blocks right at each light. Its cap
 * is a height below which light_pillars.glsl shows the pillar, and its rim toward the eye an angle above which it
 * shows. A slot is read at the light that LightPillarClusters chose for its cluster, from the center of its block.
 * A column's cap is the bottom of the lowest block in it that stops light from above the light up. The cap is the
 * light's own column's, except for a full block whose top is covered, whose light leaves through its sides: its cap is
 * the highest of its 4 side columns' caps, or OPEN_CAP where one of them has none, so a lamp set into a wall shows its
 * pillar through the column in front of it.
 * The rim follows the walls around the light. The columns within RIM_REACH blocks of the light's column each have a
 * wall where the block at the flame's level stops light, up to the top of the blocks that stop light from there up.
 * Light leaves the light toward the eye for the whole pillar, so in each of BINS directions a ray from the light meets
 * the walls' squares, and the direction's angle is the steepest of the wall tops seen from the top of the light where
 * the ray enters them, or OPEN_RIM where it meets none. The rim toward the eye blends the two directions nearest to
 * the eye.
 * A slot is built again when a block changes in one of its columns within the heights read, when a chunk of its
 * columns loads or unloads, or when it takes a new key, moves, or its full block flag changes, and it is ready to fade
 * in once built. Heights are world heights in blocks and angles are in radians, counted from +x toward +z.
 * Not thread safe.
 */
public final class LightPillarShelter {
    /**
     * The angle of a direction without a wall.
     */
    public static final double OPEN_RIM = -0.5 * Math.PI;
    public static final double OPEN_CAP = Double.POSITIVE_INFINITY;
    /**
     * Columns up to this many blocks from the light's column along each axis can hold a wall of the rim.
     */
    public static final int RIM_REACH = 2;
    /**
     * A wall of the rim is read up to this many blocks above the top of the light.
     */
    public static final int WALL_HEIGHT = 16;
    /**
     * The directions around the light whose walls are read, with direction i centered at (i + 0.5) times BIN_ANGLE,
     * so no direction from the center of a block aims at a corner of the blocks around it.
     */
    public static final int BINS = 32;
    public static final double BIN_ANGLE = 2.0 * Math.PI / BINS;
    private static final int SLOTS = LightPillarSelection.SLOT_COUNT;
    private static final int CHUNK_SIZE = 16;
    private static final int INITIAL_CHANGES = 256;
    /**
     * Walls nearer to the light than this count as this far, which keeps the angle finite where a ray enters a wall at
     * the light.
     */
    private static final double NEAREST_WALL = 0.05;
    /**
     * A wall column's square is widened by this on each side, so a ray through the corner where two columns meet
     * enters both.
     */
    private static final double SQUARE_MARGIN = 1.0e-4;
    private static final double NO_WALL = Double.NEGATIVE_INFINITY;
    private static final double NO_HIT = -1.0;
    private static final int[] SIDE_OFFSETS_X = {1, -1, 0, 0};
    private static final int[] SIDE_OFFSETS_Z = {0, 0, 1, -1};
    private static final int RIM_COLUMNS = (2 * RIM_REACH + 1) * (2 * RIM_REACH + 1) - 1;
    private static final int[] RIM_OFFSETS_X = new int[RIM_COLUMNS];
    private static final int[] RIM_OFFSETS_Z = new int[RIM_COLUMNS];
    private static final double[] BIN_COSINES = new double[BINS];
    private static final double[] BIN_SINES = new double[BINS];

    static {
        int column = 0;
        for (int offsetX = -RIM_REACH; offsetX <= RIM_REACH; offsetX++) {
            for (int offsetZ = -RIM_REACH; offsetZ <= RIM_REACH; offsetZ++) {
                if (offsetX != 0 || offsetZ != 0) {
                    RIM_OFFSETS_X[column] = offsetX;
                    RIM_OFFSETS_Z[column] = offsetZ;
                    column++;
                }
            }
        }
        for (int bin = 0; bin < BINS; bin++) {
            BIN_COSINES[bin] = Math.cos((bin + 0.5) * BIN_ANGLE);
            BIN_SINES[bin] = Math.sin((bin + 0.5) * BIN_ANGLE);
        }
    }

    private final LightPillarOccluders occluders = new LightPillarOccluders();
    private final double[] binAngles = new double[SLOTS * BINS];
    private final double[] caps = new double[SLOTS];
    private final boolean[] tracked = new boolean[SLOTS];
    private final boolean[] built = new boolean[SLOTS];
    private final boolean[] dirty = new boolean[SLOTS];
    private final boolean[] fullBlocks = new boolean[SLOTS];
    private final long[] slotKeys = new long[SLOTS];
    private final double[] originX = new double[SLOTS];
    private final double[] originY = new double[SLOTS];
    private final double[] originZ = new double[SLOTS];
    private final double[] wallTops = new double[RIM_COLUMNS];
    private int trackedCount;

    private final LongOpenHashSet changedBlockSet = new LongOpenHashSet(INITIAL_CHANGES);
    private final LongArrayList changedBlocks = new LongArrayList(INITIAL_CHANGES);
    private final LongOpenHashSet changedChunkSet = new LongOpenHashSet(INITIAL_CHANGES);
    private final LongArrayList changedChunks = new LongArrayList(INITIAL_CHANGES);

    private int blockChangesThisSecond;
    private int slotsBuiltThisSecond;
    private int blockChangesLastSecond;
    private int slotsBuiltLastSecond;

    public LightPillarOccluders occluders() {
        return occluders;
    }

    /**
     * Notes a block change at the block coordinates, unless no slot is tracked or both states stop light up to the
     * same height wherever they stand.
     */
    public void onBlockChanged(int blockX, int blockY, int blockZ, BlockState oldState, BlockState newState) {
        if (trackedCount == 0 || occluders.stopsTheSame(oldState, newState)) {
            return;
        }
        blockChangesThisSecond++;
        long block = BlockPos.asLong(blockX, blockY, blockZ);
        if (changedBlockSet.add(block)) {
            changedBlocks.add(block);
        }
    }

    /**
     * Notes that the chunk column at the chunk coordinates loaded or unloaded, unless no slot is tracked.
     */
    public void onChunkChanged(int chunkX, int chunkZ) {
        if (trackedCount == 0) {
            return;
        }
        long chunk = ChunkPos.asLong(chunkX, chunkZ);
        if (changedChunkSet.add(chunk)) {
            changedChunks.add(chunk);
        }
    }

    /**
     * Follows the slots of the selection, marks the slots that the noted changes reach, builds every marked slot, and
     * tells the selection which slots are ready to fade in. A slot takes the center of its shelter light's block and
     * the top of its cluster.
     */
    public void update(LightPillarSelection selection, LightPillarHorizons.Columns columns) {
        trackedCount = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            followSlot(selection, slot);
        }
        for (int change = 0; change < changedBlocks.size(); change++) {
            long block = changedBlocks.getLong(change);
            markBlock(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block));
        }
        for (int change = 0; change < changedChunks.size(); change++) {
            long chunk = changedChunks.getLong(change);
            markChunk(SectionPos.sectionToBlockCoord(ChunkPos.getX(chunk)),
                    SectionPos.sectionToBlockCoord(ChunkPos.getZ(chunk)));
        }
        clearChanges();
        for (int slot = 0; slot < SLOTS; slot++) {
            if (tracked[slot] && dirty[slot]) {
                build(slot, columns);
                dirty[slot] = false;
                slotsBuiltThisSecond++;
                if (!built[slot]) {
                    built[slot] = true;
                    selection.setReady(slot, true);
                }
            }
        }
    }

    private void followSlot(LightPillarSelection selection, int slot) {
        if (!selection.isUsed(slot)) {
            tracked[slot] = false;
            dirty[slot] = false;
            return;
        }
        trackedCount++;
        long key = selection.key(slot);
        double x = selection.shelterX(slot) + 0.5;
        double y = selection.top(slot);
        double z = selection.shelterZ(slot) + 0.5;
        boolean fullBlock = selection.isFullBlock(slot);
        if (!tracked[slot] || slotKeys[slot] != key) {
            tracked[slot] = true;
            built[slot] = false;
            dirty[slot] = true;
            slotKeys[slot] = key;
            setOrigin(slot, x, y, z);
            fullBlocks[slot] = fullBlock;
            selection.setReady(slot, false);
        } else if (x != originX[slot] || y != originY[slot] || z != originZ[slot] || fullBlock != fullBlocks[slot]) {
            setOrigin(slot, x, y, z);
            fullBlocks[slot] = fullBlock;
            dirty[slot] = true;
        }
    }

    private void setOrigin(int slot, double x, double y, double z) {
        originX[slot] = x;
        originY[slot] = y;
        originZ[slot] = z;
    }

    /**
     * Marks the tracked slots that read the block: the light's own column from the flame's level up, for a full block
     * also its side columns, and the other columns around it from the flame's level up to WALL_HEIGHT above it.
     */
    private void markBlock(int blockX, int blockY, int blockZ) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!tracked[slot] || dirty[slot]) {
                continue;
            }
            int offsetX = Math.abs(blockX - Mth.floor(originX[slot]));
            int offsetZ = Math.abs(blockZ - Mth.floor(originZ[slot]));
            int flameY = Mth.floor(originY[slot]);
            if (offsetX > RIM_REACH || offsetZ > RIM_REACH || blockY < flameY) {
                continue;
            }
            boolean capColumn = offsetX + offsetZ == 0 || (fullBlocks[slot] && offsetX + offsetZ == 1);
            if (capColumn || blockY <= flameY + WALL_HEIGHT) {
                dirty[slot] = true;
            }
        }
    }

    /**
     * Marks the tracked slots with a column in the chunk whose lowest corner is at the block coordinates.
     */
    private void markChunk(int minX, int minZ) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!tracked[slot]) {
                continue;
            }
            int lightX = Mth.floor(originX[slot]);
            int lightZ = Mth.floor(originZ[slot]);
            if (lightX + RIM_REACH >= minX && lightX - RIM_REACH < minX + CHUNK_SIZE
                    && lightZ + RIM_REACH >= minZ && lightZ - RIM_REACH < minZ + CHUNK_SIZE) {
                dirty[slot] = true;
            }
        }
    }

    private void build(int slot, LightPillarHorizons.Columns columns) {
        double x = originX[slot];
        double top = originY[slot];
        double z = originZ[slot];
        int lightX = Mth.floor(x);
        int lightZ = Mth.floor(z);
        double cap = readCap(columns, lightX, lightZ, top);
        if (fullBlocks[slot] && cap == Mth.ceil(top)) {
            cap = Double.NEGATIVE_INFINITY;
            for (int side = 0; side < SIDE_OFFSETS_X.length; side++) {
                cap = Math.max(cap, readCap(columns, lightX + SIDE_OFFSETS_X[side], lightZ + SIDE_OFFSETS_Z[side],
                        top));
            }
        }
        caps[slot] = cap;
        int flameY = Mth.floor(top);
        for (int column = 0; column < RIM_COLUMNS; column++) {
            wallTops[column] = readWallTop(columns, lightX + RIM_OFFSETS_X[column], lightZ + RIM_OFFSETS_Z[column],
                    flameY, top);
        }
        for (int bin = 0; bin < BINS; bin++) {
            double steepest = OPEN_RIM;
            for (int column = 0; column < RIM_COLUMNS; column++) {
                if (wallTops[column] == NO_WALL) {
                    continue;
                }
                double entry = rayEntry(x, z, BIN_COSINES[bin], BIN_SINES[bin], lightX + RIM_OFFSETS_X[column],
                        lightZ + RIM_OFFSETS_Z[column]);
                if (entry != NO_HIT) {
                    steepest = Math.max(steepest, Math.atan2(wallTops[column] - top, Math.max(entry, NEAREST_WALL)));
                }
            }
            binAngles[slot * BINS + bin] = steepest;
        }
    }

    /**
     * The distance along a level ray from the origin in the direction to where it enters the square of the column,
     * widened by SQUARE_MARGIN, 0 where it starts inside, or NO_HIT where it misses.
     */
    static double rayEntry(double originX, double originZ, double directionX, double directionZ, int columnX,
            int columnZ) {
        double nearest = 0.0;
        double farthest = Double.POSITIVE_INFINITY;
        double lowestX = columnX - SQUARE_MARGIN;
        double highestX = columnX + 1.0 + SQUARE_MARGIN;
        if (directionX == 0.0) {
            if (originX < lowestX || originX > highestX) {
                return NO_HIT;
            }
        } else {
            double first = (lowestX - originX) / directionX;
            double second = (highestX - originX) / directionX;
            nearest = Math.max(nearest, Math.min(first, second));
            farthest = Math.min(farthest, Math.max(first, second));
        }
        double lowestZ = columnZ - SQUARE_MARGIN;
        double highestZ = columnZ + 1.0 + SQUARE_MARGIN;
        if (directionZ == 0.0) {
            if (originZ < lowestZ || originZ > highestZ) {
                return NO_HIT;
            }
        } else {
            double first = (lowestZ - originZ) / directionZ;
            double second = (highestZ - originZ) / directionZ;
            nearest = Math.max(nearest, Math.min(first, second));
            farthest = Math.min(farthest, Math.max(first, second));
        }
        return nearest <= farthest ? nearest : NO_HIT;
    }

    private static double readCap(LightPillarHorizons.Columns columns, int columnX, int columnZ, double top) {
        int surface = columns.beginColumn(columnX, columnZ);
        if (surface == LightPillarHorizons.NO_BLOCKS) {
            return OPEN_CAP;
        }
        for (int blockY = Mth.ceil(top); blockY <= surface; blockY++) {
            if (columns.stoppedHeight(blockY) != LightPillarOccluders.PASSES) {
                return blockY;
            }
        }
        return OPEN_CAP;
    }

    /**
     * The top of the wall of the column, the blocks that stop light from the flame's level up, read up to WALL_HEIGHT
     * above the top of the light, or NO_WALL where the block at the flame's level lets light through.
     */
    private static double readWallTop(LightPillarHorizons.Columns columns, int columnX, int columnZ, int flameY,
            double top) {
        int surface = columns.beginColumn(columnX, columnZ);
        if (surface == LightPillarHorizons.NO_BLOCKS || flameY > surface) {
            return NO_WALL;
        }
        double wallTop = NO_WALL;
        int highestY = Math.min(surface, Mth.ceil(top + WALL_HEIGHT) - 1);
        for (int blockY = flameY; blockY <= highestY; blockY++) {
            double height = columns.stoppedHeight(blockY);
            if (height == LightPillarOccluders.PASSES) {
                break;
            }
            wallTop = blockY + height;
        }
        return wallTop;
    }

    /**
     * The rim of the slot toward the eye, an angle over the horizontal seen from the top of the light, blended
     * linearly between the two directions whose centers lie nearest to the eye's direction from the light. Directions
     * whose walls stay below the horizon count as 0, so the rim is never below 0, where it lifts no pillar, and it is 0
     * where the slot was not built since it took its key.
     */
    public double rimToward(int slot, double eyeX, double eyeZ) {
        if (!built[slot]) {
            return 0.0;
        }
        double position = Math.atan2(eyeZ - originZ[slot], eyeX - originX[slot]) / BIN_ANGLE - 0.5;
        double lowerPosition = Math.floor(position);
        double share = position - lowerPosition;
        int lowerBin = Math.floorMod((int) lowerPosition, BINS);
        int upperBin = (lowerBin + 1) % BINS;
        return (1.0 - share) * Math.max(0.0, binAngles[slot * BINS + lowerBin])
                + share * Math.max(0.0, binAngles[slot * BINS + upperBin]);
    }

    /**
     * The angle of the direction around the slot's light, or OPEN_RIM where it meets no wall or the slot was not built
     * since it took its key.
     */
    public double binAngle(int slot, int bin) {
        return built[slot] ? binAngles[slot * BINS + bin] : OPEN_RIM;
    }

    /**
     * The cap of the slot as a world height, or OPEN_CAP where it has none or was not built since it took its key.
     */
    public double cap(int slot) {
        return built[slot] ? caps[slot] : OPEN_CAP;
    }

    /**
     * Whether the slot was built since it took its key.
     */
    public boolean isBuilt(int slot) {
        return tracked[slot] && built[slot];
    }

    /**
     * Marks every tracked slot, after what stops light changed.
     */
    public void markEverySlot() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (tracked[slot]) {
                dirty[slot] = true;
            }
        }
    }

    /**
     * Forgets every slot and every noted change.
     */
    public void clear() {
        Arrays.fill(tracked, false);
        Arrays.fill(built, false);
        Arrays.fill(dirty, false);
        trackedCount = 0;
        clearChanges();
    }

    private void clearChanges() {
        changedBlockSet.clear();
        changedBlocks.clear();
        changedChunkSet.clear();
        changedChunks.clear();
    }

    /**
     * Rolls the counts of the current second into the last second.
     */
    public void rollSecond() {
        blockChangesLastSecond = blockChangesThisSecond;
        slotsBuiltLastSecond = slotsBuiltThisSecond;
        blockChangesThisSecond = 0;
        slotsBuiltThisSecond = 0;
    }

    /**
     * A line about the shelters and their work in the last full second, for the client command.
     */
    public String describe(int blocksRead) {
        int builtSlots = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (isBuilt(slot)) {
                builtSlots++;
            }
        }
        return String.format(Locale.ROOT,
                "Shelters of %d slots, %d built; last second %d block changes that stop light differently, "
                        + "%d slots built reading %d blocks",
                trackedCount, builtSlots, blockChangesLastSecond, slotsBuiltLastSecond, blocksRead);
    }

    /**
     * The cap of the slot, its rim toward the eye, and how many of its directions lift no pillar, for the client
     * command, also the last ones of a slot that fades out.
     */
    public String describeSlot(int slot, double eyeX, double eyeZ) {
        if (!built[slot]) {
            return "shelter not built";
        }
        int openBins = 0;
        for (int bin = 0; bin < BINS; bin++) {
            if (!(binAngles[slot * BINS + bin] > 0.0)) {
                openBins++;
            }
        }
        String cap = caps[slot] == OPEN_CAP ? "cap open" : String.format(Locale.ROOT, "cap y %.2f", caps[slot]);
        return String.format(Locale.ROOT, "%s, rim %.1f degrees toward the eye, %d of %d directions open", cap,
                Math.toDegrees(rimToward(slot, eyeX, eyeZ)), openBins, BINS);
    }
}
