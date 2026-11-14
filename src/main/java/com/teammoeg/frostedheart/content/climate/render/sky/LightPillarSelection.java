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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;

import java.util.Arrays;

/**
 * Chooses the clusters whose pillars look brightest and holds them in a fixed number of slots for drawing.
 * A slot fades in when its cluster is chosen and out when it no longer is, so the choice can change without pillars
 * popping. A slot stays bound to its cluster's key until it has faded out, and a newly chosen cluster waits for a free
 * slot. Each step copies a chosen cluster's lights and shelter into its slot, so a slot keeps them while it fades out
 * after its cluster is gone. A slot that takes a new key is ready to fade in, and setReady can hold its fade at 0.
 * Fades step once per client tick. Heights and distances are in blocks.
 */
public final class LightPillarSelection {
    public static final int SLOT_COUNT = 256;
    /**
     * The client ticks a slot takes to fade in or out, which is half a second.
     */
    public static final int FADE_TICKS = 10;
    /**
     * The scale height of the diamond dust above its bottom.
     */
    public static final double DUST_SCALE_HEIGHT = 80.0;
    /**
     * Closer lights are scored as if they were this far away.
     */
    static final double NEAREST_SCORED_DISTANCE = 4.0;
    private static final double SCORE_ELEVATION_TANGENT = Math.tan(Math.toRadians(10.0));
    private static final double HIGHEST_SCORE_TANGENT = Math.tan(Math.toRadians(85.0));
    private static final long NO_KEY = Long.MIN_VALUE;
    private static final int NO_SLOT = -1;
    private static final int BINS = LightPillarShelter.BINS;

    private final double[] heapScores = new double[SLOT_COUNT];
    private final int[] heapClusters = new int[SLOT_COUNT];
    private final boolean[] heapPlaced = new boolean[SLOT_COUNT];
    private int heapSize;

    private final long[] slotKeys = new long[SLOT_COUNT];
    /**
     * The slot of each key held, which never grows past its first size.
     */
    private final Long2IntOpenHashMap slotsByKey = new Long2IntOpenHashMap(2 * SLOT_COUNT);
    private final double[] slotX = new double[SLOT_COUNT];
    private final double[] slotY = new double[SLOT_COUNT];
    private final double[] slotZ = new double[SLOT_COUNT];
    private final double[] slotTop = new double[SLOT_COUNT];
    private final float[] slotRed = new float[SLOT_COUNT];
    private final float[] slotGreen = new float[SLOT_COUNT];
    private final float[] slotBlue = new float[SLOT_COUNT];
    private final double[] slotHalfExtentX = new double[SLOT_COUNT];
    private final double[] slotHalfExtentZ = new double[SLOT_COUNT];
    private final int[] slotLightCounts = new int[SLOT_COUNT];
    private final int[] slotShelterX = new int[SLOT_COUNT];
    private final int[] slotShelterZ = new int[SLOT_COUNT];
    private final boolean[] slotFullBlocks = new boolean[SLOT_COUNT];
    private final double[] slotCaps = new double[SLOT_COUNT];
    private final float[] slotRims = new float[SLOT_COUNT * BINS];
    private final float[] slotCeilings = new float[SLOT_COUNT * BINS];
    private final double[] slotScores = new double[SLOT_COUNT];
    private final int[] slotFadeTicks = new int[SLOT_COUNT];
    private final int[] slotPreviousFadeTicks = new int[SLOT_COUNT];
    private final boolean[] slotChosen = new boolean[SLOT_COUNT];
    private final boolean[] slotReady = new boolean[SLOT_COUNT];

    public LightPillarSelection() {
        slotsByKey.defaultReturnValue(NO_SLOT);
        clear();
    }

    /**
     * How bright the pillar of a cluster looks compared with others: its luminance times the dust density at the
     * crystals that show its pillar 10 degrees above the light, over its horizontal distance. The density is 1 up to
     * the dust's bottom and falls with DUST_SCALE_HEIGHT above it. The light's height is relative to the eye.
     */
    public static double score(double red, double green, double blue, double horizontalDistance, double lightHeight,
            double eyeY, double dustBottomY) {
        double distance = Math.max(horizontalDistance, NEAREST_SCORED_DISTANCE);
        double lightTangent = Math.abs(lightHeight) / distance;
        double denominator = 1.0 - lightTangent * SCORE_ELEVATION_TANGENT;
        double tangent = denominator > 0.0
                ? Math.min(HIGHEST_SCORE_TANGENT, (lightTangent + SCORE_ELEVATION_TANGENT) / denominator)
                : HIGHEST_SCORE_TANGENT;
        double crystalY = eyeY + 0.5 * (distance * tangent + lightHeight);
        double density = Math.exp(-Math.max(0.0, crystalY - dustBottomY) / DUST_SCALE_HEIGHT);
        double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        return luminance * density / distance;
    }

    /**
     * Whether a cluster with the key, the packed position of one of its lights, lies in a chunk within the radius in
     * chunks around the eye's chunk. The chunks are those whose offsets x and z from the eye's chunk satisfy
     * x * x + z * z <= radius * radius + radius.
     */
    public static boolean isWithinChunkRadius(long clusterKey, int eyeChunkX, int eyeChunkZ, int chunkRadius) {
        long offsetX = SectionPos.blockToSectionCoord(BlockPos.getX(clusterKey)) - eyeChunkX;
        long offsetZ = SectionPos.blockToSectionCoord(BlockPos.getZ(clusterKey)) - eyeChunkZ;
        return offsetX * offsetX + offsetZ * offsetZ <= (long) chunkRadius * chunkRadius + chunkRadius;
    }

    /**
     * Chooses the best SLOT_COUNT clusters by score for the eye, among the clusters within the radius in chunks around
     * the eye's chunk, and steps every slot's fade.
     */
    public void choose(LightPillarClusters clusters, double eyeX, double eyeY, double eyeZ, double dustBottomY,
            int chunkRadius) {
        heapSize = 0;
        int eyeChunkX = SectionPos.blockToSectionCoord(Mth.floor(eyeX));
        int eyeChunkZ = SectionPos.blockToSectionCoord(Mth.floor(eyeZ));
        for (int cluster = 0; cluster < clusters.count(); cluster++) {
            if (!isWithinChunkRadius(clusters.key(cluster), eyeChunkX, eyeChunkZ, chunkRadius)) {
                continue;
            }
            double offsetX = clusters.x(cluster) - eyeX;
            double offsetZ = clusters.z(cluster) - eyeZ;
            double clusterScore = score(clusters.red(cluster), clusters.green(cluster), clusters.blue(cluster),
                    Math.sqrt(offsetX * offsetX + offsetZ * offsetZ), clusters.y(cluster) - eyeY, eyeY, dustBottomY);
            if (!(clusterScore > 0.0)) {
                continue;
            }
            if (heapSize < SLOT_COUNT) {
                pushOntoHeap(clusterScore, cluster);
            } else if (clusterScore > heapScores[0]) {
                heapScores[0] = clusterScore;
                heapClusters[0] = cluster;
                siftDown();
            }
        }
        updateSlots(clusters);
    }

    /**
     * Chooses no cluster, so every slot fades out.
     */
    public void chooseNone() {
        heapSize = 0;
        updateSlots(null);
    }

    /**
     * Empties every slot at once.
     */
    public void clear() {
        heapSize = 0;
        Arrays.fill(slotKeys, NO_KEY);
        slotsByKey.clear();
        Arrays.fill(slotFadeTicks, 0);
        Arrays.fill(slotPreviousFadeTicks, 0);
        Arrays.fill(slotChosen, false);
        Arrays.fill(slotReady, false);
    }

    private void pushOntoHeap(double clusterScore, int cluster) {
        int index = heapSize++;
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heapScores[parent] <= clusterScore) {
                break;
            }
            heapScores[index] = heapScores[parent];
            heapClusters[index] = heapClusters[parent];
            index = parent;
        }
        heapScores[index] = clusterScore;
        heapClusters[index] = cluster;
    }

    private void siftDown() {
        double clusterScore = heapScores[0];
        int cluster = heapClusters[0];
        int index = 0;
        while (true) {
            int child = 2 * index + 1;
            if (child >= heapSize) {
                break;
            }
            if (child + 1 < heapSize && heapScores[child + 1] < heapScores[child]) {
                child++;
            }
            if (heapScores[child] >= clusterScore) {
                break;
            }
            heapScores[index] = heapScores[child];
            heapClusters[index] = heapClusters[child];
            index = child;
        }
        heapScores[index] = clusterScore;
        heapClusters[index] = cluster;
    }

    private void updateSlots(LightPillarClusters clusters) {
        Arrays.fill(slotChosen, false);
        for (int chosen = 0; chosen < heapSize; chosen++) {
            int slot = slotsByKey.get(clusters.key(heapClusters[chosen]));
            heapPlaced[chosen] = slot != NO_SLOT;
            if (slot != NO_SLOT) {
                fillSlot(slot, clusters, chosen);
            }
        }
        int freeSlot = 0;
        for (int chosen = 0; chosen < heapSize; chosen++) {
            if (heapPlaced[chosen]) {
                continue;
            }
            while (freeSlot < SLOT_COUNT && slotKeys[freeSlot] != NO_KEY) {
                freeSlot++;
            }
            if (freeSlot == SLOT_COUNT) {
                break;
            }
            long key = clusters.key(heapClusters[chosen]);
            slotKeys[freeSlot] = key;
            slotsByKey.put(key, freeSlot);
            slotReady[freeSlot] = true;
            fillSlot(freeSlot, clusters, chosen);
        }
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            slotPreviousFadeTicks[slot] = slotFadeTicks[slot];
            if (slotChosen[slot]) {
                if (slotReady[slot]) {
                    slotFadeTicks[slot] = Math.min(FADE_TICKS, slotFadeTicks[slot] + 1);
                }
            } else if (slotKeys[slot] != NO_KEY) {
                slotFadeTicks[slot] = Math.max(0, slotFadeTicks[slot] - 1);
                if (slotFadeTicks[slot] == 0) {
                    slotsByKey.remove(slotKeys[slot]);
                    slotKeys[slot] = NO_KEY;
                }
            }
        }
    }

    private void fillSlot(int slot, LightPillarClusters clusters, int chosen) {
        int cluster = heapClusters[chosen];
        slotX[slot] = clusters.x(cluster);
        slotY[slot] = clusters.y(cluster);
        slotZ[slot] = clusters.z(cluster);
        slotTop[slot] = clusters.top(cluster);
        slotRed[slot] = clusters.red(cluster);
        slotGreen[slot] = clusters.green(cluster);
        slotBlue[slot] = clusters.blue(cluster);
        slotHalfExtentX[slot] = clusters.halfExtentX(cluster);
        slotHalfExtentZ[slot] = clusters.halfExtentZ(cluster);
        slotLightCounts[slot] = clusters.lightCount(cluster);
        slotShelterX[slot] = clusters.shelterX(cluster);
        slotShelterZ[slot] = clusters.shelterZ(cluster);
        slotFullBlocks[slot] = clusters.isFullBlock(cluster);
        slotCaps[slot] = clusters.cap(cluster);
        clusters.copyAngles(cluster, slotRims, slotCeilings, slot * BINS);
        slotScores[slot] = heapScores[chosen];
        slotChosen[slot] = true;
    }

    /**
     * The number of clusters chosen at the last step, including those still waiting for a slot.
     */
    public int chosenCount() {
        return heapSize;
    }

    /**
     * Whether the slot holds a cluster, chosen or fading out.
     */
    public boolean isUsed(int slot) {
        return slotKeys[slot] != NO_KEY;
    }

    public int usedCount() {
        int used = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (isUsed(slot)) {
                used++;
            }
        }
        return used;
    }

    public long key(int slot) {
        return slotKeys[slot];
    }

    /**
     * Marks whether the slot may fade in while it is chosen. A slot that takes a new key is ready.
     */
    public void setReady(int slot, boolean ready) {
        slotReady[slot] = ready;
    }

    public boolean isReady(int slot) {
        return slotReady[slot];
    }

    /**
     * The slot's fade between the last two steps, from 0 to 1. A slot freed at the last step still fades during it.
     */
    public float fade(int slot, float partialTick) {
        return (slotPreviousFadeTicks[slot] + (slotFadeTicks[slot] - slotPreviousFadeTicks[slot]) * partialTick)
                / FADE_TICKS;
    }

    public double x(int slot) {
        return slotX[slot];
    }

    public double y(int slot) {
        return slotY[slot];
    }

    public double z(int slot) {
        return slotZ[slot];
    }

    /**
     * The height of the highest top of the lights of the slot's cluster.
     */
    public double top(int slot) {
        return slotTop[slot];
    }

    public float red(int slot) {
        return slotRed[slot];
    }

    public float green(int slot) {
        return slotGreen[slot];
    }

    public float blue(int slot) {
        return slotBlue[slot];
    }

    /**
     * Half the extent of the slot's cluster along x in blocks.
     */
    public double halfExtentX(int slot) {
        return slotHalfExtentX[slot];
    }

    /**
     * Half the extent of the slot's cluster along z in blocks.
     */
    public double halfExtentZ(int slot) {
        return slotHalfExtentZ[slot];
    }

    public int lightCount(int slot) {
        return slotLightCounts[slot];
    }

    /**
     * The block x of the light whose surroundings give the shelter of the slot's cluster.
     */
    public int shelterX(int slot) {
        return slotShelterX[slot];
    }

    /**
     * The block z of the light whose surroundings give the shelter of the slot's cluster.
     */
    public int shelterZ(int slot) {
        return slotShelterZ[slot];
    }

    /**
     * Whether the light that gives the shelter of the slot's cluster is a full block.
     */
    public boolean isFullBlock(int slot) {
        return slotFullBlocks[slot];
    }

    /**
     * The cap of the slot's cluster as a world height, or LightPillarShelter.OPEN_CAP.
     */
    public double cap(int slot) {
        return slotCaps[slot];
    }

    /**
     * The rim of the direction around the light that gives the slot's shelter, or LightPillarShelter.OPEN_RIM.
     */
    public double rim(int slot, int bin) {
        return slotRims[slot * BINS + bin];
    }

    /**
     * The ceiling of the direction around the light that gives the slot's shelter, or
     * LightPillarShelter.OPEN_CEILING.
     */
    public double ceiling(int slot, int bin) {
        return slotCeilings[slot * BINS + bin];
    }

    /**
     * The slot's rim toward the eye, as LightPillarShelter.rimToward gives it from the center of the light that gives
     * the slot's shelter.
     */
    public double rimToward(int slot, double eyeX, double eyeZ) {
        return LightPillarShelter.rimToward(slotRims, slot * BINS, slotShelterX[slot] + 0.5, slotShelterZ[slot] + 0.5,
                eyeX, eyeZ);
    }

    /**
     * The slot's ceiling toward the eye, as LightPillarShelter.ceilingToward gives it from the center of the light
     * that gives the slot's shelter.
     */
    public double ceilingToward(int slot, double eyeX, double eyeZ) {
        return LightPillarShelter.ceilingToward(slotCeilings, slot * BINS, slotShelterX[slot] + 0.5,
                slotShelterZ[slot] + 0.5, eyeX, eyeZ);
    }

    /**
     * A line about the slot's shelter toward the eye, for the client command.
     */
    public String describeShelter(int slot, double eyeX, double eyeZ) {
        int openBins = 0;
        for (int bin = 0; bin < BINS; bin++) {
            if (!(slotRims[slot * BINS + bin] > 0.0F)
                    && !(slotCeilings[slot * BINS + bin] < (float) LightPillarShelter.OPEN_CEILING)) {
                openBins++;
            }
        }
        return LightPillarShelter.describe(slotCaps[slot], rimToward(slot, eyeX, eyeZ),
                ceilingToward(slot, eyeX, eyeZ), openBins);
    }

    /**
     * The score of the slot's cluster when it was last chosen.
     */
    public double slotScore(int slot) {
        return slotScores[slot];
    }
}
