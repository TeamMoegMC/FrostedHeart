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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * The lights that can show pillars, merged into clusters of lights that touch and share a cap, kept per chunk section.
 * The lights of a section are added between beginSection and endSection, which replaces every cluster the section had.
 * Lights touch when their blocks differ by at most 1 along every axis, and a cluster holds the lights of the section
 * that touch each other through lights with the same cap, as LightPillarShelter.readLightCap reads it, so lights in
 * different sections never merge, and neither do lights under roofs of different heights or under a roof and open sky.
 * A cluster holds the summed strength of its lights per channel, their count, the height of their highest top, and
 * their extent across: a light that fills its block spans it, and any other light is a point at its block's center.
 * The cluster stands at the middle of that extent across and at the height of its lights' center weighted by their
 * summed strength. Its shelter is read at the light with the highest top nearest to its middle, whose full block flag
 * it carries. Its key is the packed position of its first light in the order of y, then z, then x, so it stays while
 * its lights and their caps do not change. Removing a cluster moves the last one into its index, so indices are only
 * valid until the next change.
 * A cluster also keeps the box of blocks its lights read their cap from: their columns, widened by 1 for a cluster
 * with a full block, from the lowest block above a light's top up to the cap's block, or without end where it is open.
 * A change of how high a block stops light inside the box can move the cap, so its section has to be walked again.
 */
public final class LightPillarClusters {
    /**
     * A light needs more sky light than this at its block or the block above to show a pillar.
     */
    public static final int SKY_LIGHT_THRESHOLD = 8;
    private static final int SECTION_SIZE = 16;
    private static final int SECTION_BLOCKS = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    /**
     * The bytes of a section's light data that hold its lowest layer of blocks, at two blocks per byte.
     */
    private static final int LOWEST_LAYER_BYTES = SECTION_SIZE * SECTION_SIZE / 2;
    private static final int INITIAL_SECTIONS = 1024;
    private static final int INITIAL_CLUSTERS = 4096;
    private static final int NO_INDEX = -1;
    /**
     * The offsets to the 13 blocks that touch a block and come before it in the order of y, then z, then x.
     */
    private static final int[] EARLIER_OFFSETS_X = {-1, 0, 1, -1, 0, 1, -1, 0, 1, -1, 0, 1, -1};
    private static final int[] EARLIER_OFFSETS_Y = {-1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0};
    private static final int[] EARLIER_OFFSETS_Z = {-1, -1, -1, 0, 0, 0, 1, 1, 1, -1, -1, -1, 0};
    private static final int[] SIDE_OFFSETS_X = {1, -1, 0, 0};
    private static final int[] SIDE_OFFSETS_Z = {0, 0, 1, -1};

    /**
     * The lights added since the section began, by the order they were added, and the light at each block of the
     * section, or NO_INDEX.
     */
    private final int[] lightAtBlock = new int[SECTION_BLOCKS];
    private final int[] lightBlocks = new int[SECTION_BLOCKS];
    private final double[] lightTops = new double[SECTION_BLOCKS];
    private final double[] lightCaps = new double[SECTION_BLOCKS];
    private final boolean[] lightFullBlocks = new boolean[SECTION_BLOCKS];
    private final boolean[] lightSpansBlocks = new boolean[SECTION_BLOCKS];
    private final float[] lightRed = new float[SECTION_BLOCKS];
    private final float[] lightGreen = new float[SECTION_BLOCKS];
    private final float[] lightBlue = new float[SECTION_BLOCKS];
    private final int[] lightParents = new int[SECTION_BLOCKS];
    private int lightCount;
    /**
     * The sums of each group of touching lights, kept at the index of the group's first light.
     */
    private final double[] groupWeights = new double[SECTION_BLOCKS];
    private final double[] groupWeightedY = new double[SECTION_BLOCKS];
    private final double[] groupTops = new double[SECTION_BLOCKS];
    private final double[] groupLowestX = new double[SECTION_BLOCKS];
    private final double[] groupHighestX = new double[SECTION_BLOCKS];
    private final double[] groupLowestZ = new double[SECTION_BLOCKS];
    private final double[] groupHighestZ = new double[SECTION_BLOCKS];
    private final float[] groupRed = new float[SECTION_BLOCKS];
    private final float[] groupGreen = new float[SECTION_BLOCKS];
    private final float[] groupBlue = new float[SECTION_BLOCKS];
    private final int[] groupCounts = new int[SECTION_BLOCKS];
    private final int[] groupLowestCapY = new int[SECTION_BLOCKS];
    private final boolean[] groupHasFullBlocks = new boolean[SECTION_BLOCKS];
    private final int[] groupShelterLights = new int[SECTION_BLOCKS];
    private final double[] groupShelterDistances = new double[SECTION_BLOCKS];
    private final long[] removedKeys = new long[SECTION_BLOCKS];
    private int sectionX;
    private int sectionY;
    private int sectionZ;

    /**
     * For each section with clusters, the index of the first cluster of its list, which the clusters of a section
     * form through nextInSection and previousInSection.
     */
    private final Long2IntOpenHashMap sectionFirstClusters = new Long2IntOpenHashMap(INITIAL_SECTIONS);
    private final Long2IntOpenHashMap clusterIndices = new Long2IntOpenHashMap(INITIAL_CLUSTERS);
    /**
     * The number of clusters in each chunk column that has any, so a block change in a column without clusters
     * needs no look at its sections.
     */
    private final Long2IntOpenHashMap columnClusterCounts = new Long2IntOpenHashMap(INITIAL_SECTIONS);
    private long[] keys = new long[INITIAL_CLUSTERS];
    private long[] sections = new long[INITIAL_CLUSTERS];
    private int[] nextInSection = new int[INITIAL_CLUSTERS];
    private int[] previousInSection = new int[INITIAL_CLUSTERS];
    private double[] x = new double[INITIAL_CLUSTERS];
    private double[] y = new double[INITIAL_CLUSTERS];
    private double[] z = new double[INITIAL_CLUSTERS];
    private double[] top = new double[INITIAL_CLUSTERS];
    private float[] red = new float[INITIAL_CLUSTERS];
    private float[] green = new float[INITIAL_CLUSTERS];
    private float[] blue = new float[INITIAL_CLUSTERS];
    private double[] halfExtentX = new double[INITIAL_CLUSTERS];
    private double[] halfExtentZ = new double[INITIAL_CLUSTERS];
    private int[] lightCounts = new int[INITIAL_CLUSTERS];
    private int[] shelterX = new int[INITIAL_CLUSTERS];
    private int[] shelterZ = new int[INITIAL_CLUSTERS];
    private boolean[] fullBlocks = new boolean[INITIAL_CLUSTERS];
    private int[] capLowestX = new int[INITIAL_CLUSTERS];
    private int[] capHighestX = new int[INITIAL_CLUSTERS];
    private int[] capLowestY = new int[INITIAL_CLUSTERS];
    private int[] capHighestY = new int[INITIAL_CLUSTERS];
    private int[] capLowestZ = new int[INITIAL_CLUSTERS];
    private int[] capHighestZ = new int[INITIAL_CLUSTERS];
    private int size;

    public LightPillarClusters() {
        sectionFirstClusters.defaultReturnValue(NO_INDEX);
        clusterIndices.defaultReturnValue(NO_INDEX);
        Arrays.fill(lightAtBlock, NO_INDEX);
    }

    /**
     * Whether a section can hold a light with enough sky light for a pillar, from the sky light data of the section
     * and of the section above, each null where the light engine stores none. A light counts by the sky light at its
     * own block or the block above, so the lowest layer of the section above counts too. Missing data counts as lit,
     * because the sky light engine takes the light of a section without data from the sections above it.
     */
    public static boolean sectionCanShowPillars(@Nullable DataLayer sectionSkyLight, @Nullable DataLayer aboveSkyLight) {
        return hasLightAboveThreshold(sectionSkyLight, DataLayer.SIZE)
                || hasLightAboveThreshold(aboveSkyLight, LOWEST_LAYER_BYTES);
    }

    /**
     * Whether the first bytes of the light data hold a value above SKY_LIGHT_THRESHOLD.
     */
    private static boolean hasLightAboveThreshold(@Nullable DataLayer light, int byteCount) {
        if (light == null) {
            return true;
        }
        if (light.isDefinitelyHomogenous()) {
            return light.get(0, 0, 0) > SKY_LIGHT_THRESHOLD;
        }
        byte[] data = light.getData();
        for (int index = 0; index < byteCount; index++) {
            int pair = data[index];
            if ((pair & 0xF) > SKY_LIGHT_THRESHOLD || ((pair >> 4) & 0xF) > SKY_LIGHT_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Starts adding the lights of the chunk section at the section coordinates, dropping lights added since a section
     * began without ending.
     */
    public void beginSection(int sectionX, int sectionY, int sectionZ) {
        resetLights();
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
    }

    /**
     * Adds a light at the block, which must lie in the section begun last, with the height of its top, from which its
     * light leaves, whether it is a full block, whether it spans its block across the view, its cap as a world height
     * or LightPillarShelter.OPEN_CAP, and its strength per channel. A second light at the same block is left out.
     */
    public void addLight(int blockX, int blockY, int blockZ, double lightTop, boolean fullBlock, boolean spansBlock,
            double cap, float red, float green, float blue) {
        int block = blockIndex(blockX & (SECTION_SIZE - 1), blockY & (SECTION_SIZE - 1), blockZ & (SECTION_SIZE - 1));
        if (lightAtBlock[block] != NO_INDEX) {
            return;
        }
        int light = lightCount++;
        lightAtBlock[block] = light;
        lightBlocks[light] = block;
        lightTops[light] = lightTop;
        lightCaps[light] = cap;
        lightFullBlocks[light] = fullBlock;
        lightSpansBlocks[light] = spansBlock;
        lightRed[light] = red;
        lightGreen[light] = green;
        lightBlue[light] = blue;
        lightParents[light] = light;
    }

    /**
     * Ends the section begun last and replaces its clusters with one for each group of its lights that touch through
     * lights with the same cap.
     */
    public void endSection() {
        removeSection(sectionX, sectionY, sectionZ);
        for (int light = 0; light < lightCount; light++) {
            joinEarlierNeighbors(light);
        }
        for (int light = 0; light < lightCount; light++) {
            if (findGroup(light) == light) {
                startGroup(light);
            }
        }
        for (int light = 0; light < lightCount; light++) {
            addToGroup(findGroup(light), light);
        }
        for (int light = 0; light < lightCount; light++) {
            chooseShelterLight(findGroup(light), light);
        }
        long section = SectionPos.asLong(sectionX, sectionY, sectionZ);
        for (int light = 0; light < lightCount; light++) {
            if (lightParents[light] == light && groupWeights[light] > 0.0) {
                addGroup(section, light);
            }
        }
        resetLights();
    }

    private static int blockIndex(int localX, int localY, int localZ) {
        return localX + SECTION_SIZE * (localZ + SECTION_SIZE * localY);
    }

    private void joinEarlierNeighbors(int light) {
        int block = lightBlocks[light];
        int localX = block & (SECTION_SIZE - 1);
        int localZ = (block / SECTION_SIZE) & (SECTION_SIZE - 1);
        int localY = block / (SECTION_SIZE * SECTION_SIZE);
        for (int neighbor = 0; neighbor < EARLIER_OFFSETS_X.length; neighbor++) {
            int neighborX = localX + EARLIER_OFFSETS_X[neighbor];
            int neighborY = localY + EARLIER_OFFSETS_Y[neighbor];
            int neighborZ = localZ + EARLIER_OFFSETS_Z[neighbor];
            if (neighborX < 0 || neighborX >= SECTION_SIZE || neighborY < 0 || neighborZ < 0
                    || neighborZ >= SECTION_SIZE) {
                continue;
            }
            int other = lightAtBlock[blockIndex(neighborX, neighborY, neighborZ)];
            if (other != NO_INDEX && lightCaps[other] == lightCaps[light]) {
                joinGroups(light, other);
            }
        }
    }

    /**
     * Joins the groups of two lights under the group's light with the lower block index, so every group is kept at
     * its first light in the order of y, then z, then x.
     */
    private void joinGroups(int light, int other) {
        int group = findGroup(light);
        int otherGroup = findGroup(other);
        if (group == otherGroup) {
            return;
        }
        if (lightBlocks[group] < lightBlocks[otherGroup]) {
            lightParents[otherGroup] = group;
        } else {
            lightParents[group] = otherGroup;
        }
    }

    private int findGroup(int light) {
        int group = light;
        while (lightParents[group] != group) {
            lightParents[group] = lightParents[lightParents[group]];
            group = lightParents[group];
        }
        return group;
    }

    private void startGroup(int group) {
        groupWeights[group] = 0.0;
        groupWeightedY[group] = 0.0;
        groupTops[group] = Double.NEGATIVE_INFINITY;
        groupLowestX[group] = Double.POSITIVE_INFINITY;
        groupHighestX[group] = Double.NEGATIVE_INFINITY;
        groupLowestZ[group] = Double.POSITIVE_INFINITY;
        groupHighestZ[group] = Double.NEGATIVE_INFINITY;
        groupRed[group] = 0.0F;
        groupGreen[group] = 0.0F;
        groupBlue[group] = 0.0F;
        groupCounts[group] = 0;
        groupLowestCapY[group] = Integer.MAX_VALUE;
        groupHasFullBlocks[group] = false;
        groupShelterLights[group] = NO_INDEX;
        groupShelterDistances[group] = Double.POSITIVE_INFINITY;
    }

    private void addToGroup(int group, int light) {
        int block = lightBlocks[light];
        double centerX = (block & (SECTION_SIZE - 1)) + 0.5;
        double centerZ = ((block / SECTION_SIZE) & (SECTION_SIZE - 1)) + 0.5;
        double centerY = block / (SECTION_SIZE * SECTION_SIZE) + 0.5;
        double halfSpan = lightSpansBlocks[light] ? 0.5 : 0.0;
        double weight = lightRed[light] + lightGreen[light] + lightBlue[light];
        groupWeights[group] += weight;
        groupWeightedY[group] += weight * centerY;
        groupTops[group] = Math.max(groupTops[group], lightTops[light]);
        groupLowestX[group] = Math.min(groupLowestX[group], centerX - halfSpan);
        groupHighestX[group] = Math.max(groupHighestX[group], centerX + halfSpan);
        groupLowestZ[group] = Math.min(groupLowestZ[group], centerZ - halfSpan);
        groupHighestZ[group] = Math.max(groupHighestZ[group], centerZ + halfSpan);
        groupRed[group] += lightRed[light];
        groupGreen[group] += lightGreen[light];
        groupBlue[group] += lightBlue[light];
        groupCounts[group]++;
        groupLowestCapY[group] = Math.min(groupLowestCapY[group], Mth.ceil(lightTops[light]));
        groupHasFullBlocks[group] |= lightFullBlocks[light];
    }

    /**
     * Takes the light for the group's shelter if it has the group's highest top and lies nearer to the middle of the
     * group's extent than the one taken so far, or as near with a lower block index.
     */
    private void chooseShelterLight(int group, int light) {
        if (lightTops[light] != groupTops[group]) {
            return;
        }
        int block = lightBlocks[light];
        double offsetX = (block & (SECTION_SIZE - 1)) + 0.5 - 0.5 * (groupLowestX[group] + groupHighestX[group]);
        double offsetZ = ((block / SECTION_SIZE) & (SECTION_SIZE - 1)) + 0.5
                - 0.5 * (groupLowestZ[group] + groupHighestZ[group]);
        double distance = offsetX * offsetX + offsetZ * offsetZ;
        int chosen = groupShelterLights[group];
        if (chosen == NO_INDEX || distance < groupShelterDistances[group]
                || (distance == groupShelterDistances[group] && block < lightBlocks[chosen])) {
            groupShelterLights[group] = light;
            groupShelterDistances[group] = distance;
        }
    }

    private void addGroup(long section, int group) {
        int originX = SectionPos.sectionToBlockCoord(sectionX);
        int originY = SectionPos.sectionToBlockCoord(sectionY);
        int originZ = SectionPos.sectionToBlockCoord(sectionZ);
        int block = lightBlocks[group];
        long key = BlockPos.asLong(originX + (block & (SECTION_SIZE - 1)),
                originY + block / (SECTION_SIZE * SECTION_SIZE),
                originZ + ((block / SECTION_SIZE) & (SECTION_SIZE - 1)));
        int shelterLight = groupShelterLights[group];
        int shelterBlock = lightBlocks[shelterLight];
        int cluster = append(section, key);
        x[cluster] = originX + 0.5 * (groupLowestX[group] + groupHighestX[group]);
        y[cluster] = originY + groupWeightedY[group] / groupWeights[group];
        z[cluster] = originZ + 0.5 * (groupLowestZ[group] + groupHighestZ[group]);
        top[cluster] = groupTops[group];
        red[cluster] = groupRed[group];
        green[cluster] = groupGreen[group];
        blue[cluster] = groupBlue[group];
        halfExtentX[cluster] = 0.5 * (groupHighestX[group] - groupLowestX[group]);
        halfExtentZ[cluster] = 0.5 * (groupHighestZ[group] - groupLowestZ[group]);
        lightCounts[cluster] = groupCounts[group];
        shelterX[cluster] = originX + (shelterBlock & (SECTION_SIZE - 1));
        shelterZ[cluster] = originZ + ((shelterBlock / SECTION_SIZE) & (SECTION_SIZE - 1));
        fullBlocks[cluster] = lightFullBlocks[shelterLight];
        // The columns under an extent run from the floor of its lowest edge to one below the ceiling of its highest:
        int sideReach = groupHasFullBlocks[group] ? 1 : 0;
        capLowestX[cluster] = originX + Mth.floor(groupLowestX[group]) - sideReach;
        capHighestX[cluster] = originX + Mth.ceil(groupHighestX[group]) - 1 + sideReach;
        capLowestZ[cluster] = originZ + Mth.floor(groupLowestZ[group]) - sideReach;
        capHighestZ[cluster] = originZ + Mth.ceil(groupHighestZ[group]) - 1 + sideReach;
        capLowestY[cluster] = groupLowestCapY[group];
        double cap = lightCaps[group];
        capHighestY[cluster] = cap == LightPillarShelter.OPEN_CAP ? Integer.MAX_VALUE : Mth.floor(cap);
    }

    /**
     * Removes the clusters of the chunk section at the section coordinates.
     */
    public void removeSection(int sectionX, int sectionY, int sectionZ) {
        int removedCount = 0;
        for (int cluster = sectionFirstClusters.get(SectionPos.asLong(sectionX, sectionY, sectionZ));
                cluster != NO_INDEX; cluster = nextInSection[cluster]) {
            removedKeys[removedCount++] = keys[cluster];
        }
        for (int removed = 0; removed < removedCount; removed++) {
            remove(removedKeys[removed]);
        }
    }

    /**
     * Removes the clusters of the sections of a chunk column, from the lowest section up through the count.
     */
    public void removeColumn(int chunkX, int chunkZ, int lowestSectionY, int sectionCount) {
        for (int sectionY = lowestSectionY; sectionY < lowestSectionY + sectionCount; sectionY++) {
            removeSection(chunkX, sectionY, chunkZ);
        }
    }

    /**
     * Removes every cluster.
     */
    public void clear() {
        sectionFirstClusters.clear();
        clusterIndices.clear();
        columnClusterCounts.clear();
        size = 0;
        resetLights();
    }

    /**
     * Queues the sections with a cluster that reads its cap at the block, after the block changed how high it stops
     * light. They lie at or below the block's section, in its chunk column or, through the side columns of a full
     * block, in a chunk column beside it. Returns how many sections were newly queued.
     */
    public int queueSectionsReadingCapAt(int blockX, int blockY, int blockZ, LightPillarWorkQueue queue) {
        if (size == 0 || !queue.isTracking()) {
            return 0;
        }
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        int queued = queueColumnReadingCapAt(chunkX, chunkZ, blockX, blockY, blockZ, queue);
        for (int side = 0; side < SIDE_OFFSETS_X.length; side++) {
            int sideChunkX = SectionPos.blockToSectionCoord(blockX + SIDE_OFFSETS_X[side]);
            int sideChunkZ = SectionPos.blockToSectionCoord(blockZ + SIDE_OFFSETS_Z[side]);
            if (sideChunkX != chunkX || sideChunkZ != chunkZ) {
                queued += queueColumnReadingCapAt(sideChunkX, sideChunkZ, blockX, blockY, blockZ, queue);
            }
        }
        return queued;
    }

    private int queueColumnReadingCapAt(int chunkX, int chunkZ, int blockX, int blockY, int blockZ,
            LightPillarWorkQueue queue) {
        if (!columnClusterCounts.containsKey(ChunkPos.asLong(chunkX, chunkZ))) {
            return 0;
        }
        int queued = 0;
        int highestSectionY = Math.min(SectionPos.blockToSectionCoord(blockY),
                queue.lowestSectionY() + queue.sectionCount() - 1);
        for (int sectionY = highestSectionY; sectionY >= queue.lowestSectionY(); sectionY--) {
            long section = SectionPos.asLong(chunkX, sectionY, chunkZ);
            for (int cluster = sectionFirstClusters.get(section); cluster != NO_INDEX;
                    cluster = nextInSection[cluster]) {
                if (blockX >= capLowestX[cluster] && blockX <= capHighestX[cluster]
                        && blockZ >= capLowestZ[cluster] && blockZ <= capHighestZ[cluster]
                        && blockY >= capLowestY[cluster] && blockY <= capHighestY[cluster]) {
                    if (queue.queueSection(section)) {
                        queued++;
                    }
                    break;
                }
            }
        }
        return queued;
    }

    /**
     * Queues the sections beside the chunk column at the chunk coordinates with a cluster that reads its cap in it,
     * through the side columns of a full block, once the column's blocks can be read. Returns how many sections were
     * newly queued.
     */
    public int queueSectionsReadingCapsIn(int chunkX, int chunkZ, LightPillarWorkQueue queue) {
        if (size == 0 || !queue.isTracking()) {
            return 0;
        }
        int lowestX = SectionPos.sectionToBlockCoord(chunkX);
        int lowestZ = SectionPos.sectionToBlockCoord(chunkZ);
        int highestX = lowestX + SECTION_SIZE - 1;
        int highestZ = lowestZ + SECTION_SIZE - 1;
        int queued = 0;
        for (int side = 0; side < SIDE_OFFSETS_X.length; side++) {
            int sideChunkX = chunkX + SIDE_OFFSETS_X[side];
            int sideChunkZ = chunkZ + SIDE_OFFSETS_Z[side];
            if (!columnClusterCounts.containsKey(ChunkPos.asLong(sideChunkX, sideChunkZ))) {
                continue;
            }
            for (int sectionY = queue.lowestSectionY(); sectionY < queue.lowestSectionY() + queue.sectionCount();
                    sectionY++) {
                long section = SectionPos.asLong(sideChunkX, sectionY, sideChunkZ);
                for (int cluster = sectionFirstClusters.get(section); cluster != NO_INDEX;
                        cluster = nextInSection[cluster]) {
                    if (capHighestX[cluster] >= lowestX && capLowestX[cluster] <= highestX
                            && capHighestZ[cluster] >= lowestZ && capLowestZ[cluster] <= highestZ) {
                        if (queue.queueSection(section)) {
                            queued++;
                        }
                        break;
                    }
                }
            }
        }
        return queued;
    }

    /**
     * Queues every section with a cluster, after what stops light changed. Returns how many were newly queued.
     */
    public int queueEverySection(LightPillarWorkQueue queue) {
        int queued = 0;
        for (int cluster = 0; cluster < size; cluster++) {
            if (queue.queueSection(sections[cluster])) {
                queued++;
            }
        }
        return queued;
    }

    /**
     * Adds a cluster with the key at the end and at the front of its section's list, and returns its index.
     */
    private int append(long section, long key) {
        if (size == keys.length) {
            grow(2 * size);
        }
        int cluster = size++;
        int first = sectionFirstClusters.get(section);
        keys[cluster] = key;
        sections[cluster] = section;
        nextInSection[cluster] = first;
        previousInSection[cluster] = NO_INDEX;
        if (first != NO_INDEX) {
            previousInSection[first] = cluster;
        }
        sectionFirstClusters.put(section, cluster);
        clusterIndices.put(key, cluster);
        columnClusterCounts.addTo(ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section)), 1);
        return cluster;
    }

    private void grow(int capacity) {
        keys = Arrays.copyOf(keys, capacity);
        sections = Arrays.copyOf(sections, capacity);
        nextInSection = Arrays.copyOf(nextInSection, capacity);
        previousInSection = Arrays.copyOf(previousInSection, capacity);
        x = Arrays.copyOf(x, capacity);
        y = Arrays.copyOf(y, capacity);
        z = Arrays.copyOf(z, capacity);
        top = Arrays.copyOf(top, capacity);
        red = Arrays.copyOf(red, capacity);
        green = Arrays.copyOf(green, capacity);
        blue = Arrays.copyOf(blue, capacity);
        halfExtentX = Arrays.copyOf(halfExtentX, capacity);
        halfExtentZ = Arrays.copyOf(halfExtentZ, capacity);
        lightCounts = Arrays.copyOf(lightCounts, capacity);
        shelterX = Arrays.copyOf(shelterX, capacity);
        shelterZ = Arrays.copyOf(shelterZ, capacity);
        fullBlocks = Arrays.copyOf(fullBlocks, capacity);
        capLowestX = Arrays.copyOf(capLowestX, capacity);
        capHighestX = Arrays.copyOf(capHighestX, capacity);
        capLowestY = Arrays.copyOf(capLowestY, capacity);
        capHighestY = Arrays.copyOf(capHighestY, capacity);
        capLowestZ = Arrays.copyOf(capLowestZ, capacity);
        capHighestZ = Arrays.copyOf(capHighestZ, capacity);
    }

    /**
     * Takes the cluster with the key out of its section's list and moves the last cluster into its index.
     */
    private void remove(long key) {
        int cluster = clusterIndices.remove(key);
        if (cluster == NO_INDEX) {
            return;
        }
        unlink(cluster);
        long column = ChunkPos.asLong(SectionPos.x(sections[cluster]), SectionPos.z(sections[cluster]));
        if (columnClusterCounts.addTo(column, -1) <= 1) {
            columnClusterCounts.remove(column);
        }
        size--;
        if (cluster == size) {
            return;
        }
        int last = size;
        keys[cluster] = keys[last];
        sections[cluster] = sections[last];
        nextInSection[cluster] = nextInSection[last];
        previousInSection[cluster] = previousInSection[last];
        x[cluster] = x[last];
        y[cluster] = y[last];
        z[cluster] = z[last];
        top[cluster] = top[last];
        red[cluster] = red[last];
        green[cluster] = green[last];
        blue[cluster] = blue[last];
        halfExtentX[cluster] = halfExtentX[last];
        halfExtentZ[cluster] = halfExtentZ[last];
        lightCounts[cluster] = lightCounts[last];
        shelterX[cluster] = shelterX[last];
        shelterZ[cluster] = shelterZ[last];
        fullBlocks[cluster] = fullBlocks[last];
        capLowestX[cluster] = capLowestX[last];
        capHighestX[cluster] = capHighestX[last];
        capLowestY[cluster] = capLowestY[last];
        capHighestY[cluster] = capHighestY[last];
        capLowestZ[cluster] = capLowestZ[last];
        capHighestZ[cluster] = capHighestZ[last];
        if (previousInSection[cluster] != NO_INDEX) {
            nextInSection[previousInSection[cluster]] = cluster;
        } else {
            sectionFirstClusters.put(sections[cluster], cluster);
        }
        if (nextInSection[cluster] != NO_INDEX) {
            previousInSection[nextInSection[cluster]] = cluster;
        }
        clusterIndices.put(keys[cluster], cluster);
    }

    /**
     * Takes the cluster out of its section's list, and drops the section's list once it is empty.
     */
    private void unlink(int cluster) {
        int previous = previousInSection[cluster];
        int next = nextInSection[cluster];
        if (previous != NO_INDEX) {
            nextInSection[previous] = next;
        } else if (next != NO_INDEX) {
            sectionFirstClusters.put(sections[cluster], next);
        } else {
            sectionFirstClusters.remove(sections[cluster]);
        }
        if (next != NO_INDEX) {
            previousInSection[next] = previous;
        }
    }

    private void resetLights() {
        for (int light = 0; light < lightCount; light++) {
            lightAtBlock[lightBlocks[light]] = NO_INDEX;
        }
        lightCount = 0;
    }

    /**
     * The number of clusters.
     */
    public int count() {
        return size;
    }

    /**
     * The number of chunk sections with clusters.
     */
    public int sectionCount() {
        return sectionFirstClusters.size();
    }

    public long key(int cluster) {
        return keys[cluster];
    }

    /**
     * The middle of the cluster's extent along x.
     */
    public double x(int cluster) {
        return x[cluster];
    }

    /**
     * The height of the center of the cluster's lights weighted by their summed strength.
     */
    public double y(int cluster) {
        return y[cluster];
    }

    /**
     * The middle of the cluster's extent along z.
     */
    public double z(int cluster) {
        return z[cluster];
    }

    /**
     * The height of the highest top of the cluster's lights.
     */
    public double top(int cluster) {
        return top[cluster];
    }

    public float red(int cluster) {
        return red[cluster];
    }

    public float green(int cluster) {
        return green[cluster];
    }

    public float blue(int cluster) {
        return blue[cluster];
    }

    /**
     * Half the cluster's extent along x in blocks, 0 for a single light that does not span its block.
     */
    public double halfExtentX(int cluster) {
        return halfExtentX[cluster];
    }

    /**
     * Half the cluster's extent along z in blocks.
     */
    public double halfExtentZ(int cluster) {
        return halfExtentZ[cluster];
    }

    public int lightCount(int cluster) {
        return lightCounts[cluster];
    }

    /**
     * The block x of the light whose column and surroundings give the cluster's shelter.
     */
    public int shelterX(int cluster) {
        return shelterX[cluster];
    }

    /**
     * The block z of the light whose column and surroundings give the cluster's shelter.
     */
    public int shelterZ(int cluster) {
        return shelterZ[cluster];
    }

    /**
     * Whether the light that gives the cluster's shelter is a full block.
     */
    public boolean isFullBlock(int cluster) {
        return fullBlocks[cluster];
    }
}
