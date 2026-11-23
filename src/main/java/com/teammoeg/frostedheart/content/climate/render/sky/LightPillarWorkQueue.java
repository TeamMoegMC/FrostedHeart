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

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The chunk sections whose lights must be found again, kept from the world changes that hooks report, so nothing has
 * to poll the world. Positions are packed as by BlockPos, SectionPos and ChunkPos.
 * A chunk column counts as loaded from its load to its unload. It waits for its sky light after each load, because
 * until the light arrives every block reads full sky light, and then every section of it is queued.
 * After that a section is queued when its sky light changes, together with the section below, whose top layer reads
 * the light of the blocks above it, and when a block that shows pillars appears, goes away, or changes its block or
 * its emission. LightPillarClusters also queues the sections whose clusters read their shelter at a changed block or in
 * a column that got its light, and every section with clusters when the block tags change.
 * Sections are queued only while tracking, each at most once until it is taken, and unloaded columns are
 * kept until taken so their clusters can be dropped. Loaded columns are kept also while not tracking, so tracking can
 * start at any time. Not thread safe.
 */
public final class LightPillarWorkQueue {
    private static final int INITIAL_COLUMNS = 1024;
    private static final int INITIAL_SECTIONS = 4096;

    private final LongOpenHashSet loadedColumns = new LongOpenHashSet(INITIAL_COLUMNS);
    private final LongOpenHashSet columnsAwaitingLight = new LongOpenHashSet(INITIAL_COLUMNS);
    private final LongOpenHashSet queuedSections = new LongOpenHashSet(INITIAL_SECTIONS);
    private final LongArrayFIFOQueue sectionOrder = new LongArrayFIFOQueue(INITIAL_SECTIONS);
    private final LongArrayFIFOQueue droppedColumns = new LongArrayFIFOQueue(INITIAL_COLUMNS);
    private boolean tracking;
    private int lowestSectionY;
    private int sectionCount;
    private long takenSection;
    private long takenColumn;

    /**
     * Whether a block change can change the lights that show pillars: a block that shows pillars appears, goes away,
     * or turns into another block or another emission. Flowing lava changing its level and fire changing its age
     * change nothing.
     */
    @SuppressWarnings("deprecation")
    public static boolean changesPillars(BlockState oldState, BlockState newState) {
        if (!LightPillarEmitters.showsPillar(oldState) && !LightPillarEmitters.showsPillar(newState)) {
            return false;
        }
        return oldState.getBlock() != newState.getBlock()
                || oldState.getLightEmission() != newState.getLightEmission();
    }

    /**
     * Notes that a chunk column's blocks arrived, which also happens again for a column that is loaded.
     */
    public void onColumnLoaded(long column) {
        loadedColumns.add(column);
        columnsAwaitingLight.add(column);
    }

    /**
     * Notes that a chunk column's sky light arrived, and queues all its sections when it was waiting for it.
     * Returns whether it was waiting.
     */
    public boolean onColumnLit(long column) {
        if (!columnsAwaitingLight.remove(column)) {
            return false;
        }
        if (tracking) {
            queueColumn(column);
        }
        return true;
    }

    /**
     * Notes that a chunk column was unloaded.
     */
    public void onColumnUnloaded(long column) {
        loadedColumns.remove(column);
        columnsAwaitingLight.remove(column);
        if (tracking) {
            droppedColumns.enqueue(column);
        }
    }

    /**
     * Queues a section whose sky light changed and the section below it. Returns how many were newly queued.
     */
    public int onSkyLightChanged(long section) {
        int queued = queueSection(section) ? 1 : 0;
        if (queueSection(SectionPos.offset(section, 0, -1, 0))) {
            queued++;
        }
        return queued;
    }

    /**
     * Queues the section of a block whose state changed, when the change can change the lights that show pillars.
     * Returns whether the section was newly queued.
     */
    public boolean onBlockChanged(long blockPosition, BlockState oldState, BlockState newState) {
        return changesPillars(oldState, newState) && queueSection(SectionPos.blockToSection(blockPosition));
    }

    /**
     * Starts queueing sections, for a level whose sections run from the lowest section height through the count, and
     * queues every section of each loaded column whose sky light has arrived. Returns the number of those columns.
     * Called once, when tracking starts. It allocates an iterator.
     */
    public int startTracking(int lowestSectionY, int sectionCount) {
        clearWork();
        tracking = true;
        this.lowestSectionY = lowestSectionY;
        this.sectionCount = sectionCount;
        int columns = 0;
        LongIterator iterator = loadedColumns.iterator();
        while (iterator.hasNext()) {
            long column = iterator.nextLong();
            if (!columnsAwaitingLight.contains(column)) {
                queueColumn(column);
                columns++;
            }
        }
        return columns;
    }

    /**
     * Stops queueing sections and forgets the queued work, keeping the loaded columns.
     */
    public void stopTracking() {
        tracking = false;
        clearWork();
    }

    /**
     * Stops tracking and forgets the loaded columns too, for a new level.
     */
    public void reset() {
        stopTracking();
        loadedColumns.clear();
        columnsAwaitingLight.clear();
    }

    private void clearWork() {
        sectionOrder.clear();
        queuedSections.clear();
        droppedColumns.clear();
    }

    private void queueColumn(long column) {
        int chunkX = ChunkPos.getX(column);
        int chunkZ = ChunkPos.getZ(column);
        for (int sectionY = lowestSectionY; sectionY < lowestSectionY + sectionCount; sectionY++) {
            queueSection(SectionPos.asLong(chunkX, sectionY, chunkZ));
        }
    }

    /**
     * Queues a section while tracking, unless it lies outside the level, its column is not loaded or waits for its
     * light, or it is queued already. Returns whether it was newly queued.
     */
    public boolean queueSection(long section) {
        if (!tracking) {
            return false;
        }
        int sectionY = SectionPos.y(section);
        if (sectionY < lowestSectionY || sectionY >= lowestSectionY + sectionCount) {
            return false;
        }
        long column = ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section));
        if (!loadedColumns.contains(column) || columnsAwaitingLight.contains(column)) {
            return false;
        }
        if (!queuedSections.add(section)) {
            return false;
        }
        sectionOrder.enqueue(section);
        return true;
    }

    /**
     * Takes the next queued section of a loaded column whose sky light has arrived, readable with takenSection, and
     * skips the others. Returns false when none is left.
     */
    public boolean nextSection() {
        while (!sectionOrder.isEmpty()) {
            long section = sectionOrder.dequeueLong();
            queuedSections.remove(section);
            long column = ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section));
            if (loadedColumns.contains(column) && !columnsAwaitingLight.contains(column)) {
                takenSection = section;
                return true;
            }
        }
        return false;
    }

    public long takenSection() {
        return takenSection;
    }

    /**
     * Takes the next column unloaded while tracking, readable with takenColumn. Returns false when none is left.
     */
    public boolean nextDroppedColumn() {
        if (droppedColumns.isEmpty()) {
            return false;
        }
        takenColumn = droppedColumns.dequeueLong();
        return true;
    }

    public long takenColumn() {
        return takenColumn;
    }

    public boolean isTracking() {
        return tracking;
    }

    /**
     * The height of the level's lowest section, as given when tracking started.
     */
    public int lowestSectionY() {
        return lowestSectionY;
    }

    /**
     * The number of the level's sections, as given when tracking started.
     */
    public int sectionCount() {
        return sectionCount;
    }

    public int queuedSectionCount() {
        return queuedSections.size();
    }

    public int loadedColumnCount() {
        return loadedColumns.size();
    }

    public boolean isLoaded(long column) {
        return loadedColumns.contains(column);
    }

    public boolean isAwaitingLight(long column) {
        return columnsAwaitingLight.contains(column);
    }
}
