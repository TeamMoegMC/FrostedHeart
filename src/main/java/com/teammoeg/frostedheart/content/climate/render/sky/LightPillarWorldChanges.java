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

import com.teammoeg.frostedheart.FHMain;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

/**
 * Passes the changes in the client world that light pillars depend on, as hooks report them, to a
 * LightPillarWorkQueue and to the LightPillarShelter, so nothing has to poll the world. Changes of what stops light
 * also queue the sections whose clusters read their cap there, as LightPillarClusters finds them. Block changes come
 * from ClientLevel.setBlocksDirty, sky light computed on the client from ClientChunkCache.onLightUpdate, sky light
 * received from the server from ClientPacketListener.readSectionList, and chunk columns from the Forge chunk events.
 * The hooks only queue, and the client tick takes the work. The queue follows the level of the latest hook call, and
 * it queues sections only while LightPillarSources tracks. Client thread only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightPillarWorldChanges {
    private static final int TICKS_PER_SECOND = 20;
    private static final int LISTED_BLOCKS = 3;
    private static final long NO_POSITION = Long.MIN_VALUE;

    private static final LightPillarWorkQueue queue = new LightPillarWorkQueue();
    private static final Counts currentSecond = new Counts();
    private static final Counts lastSecond = new Counts();

    /**
     * Block changes per block of the current and of the last second, counted for the new state or for the old one
     * when the new one is air. Changes at lights are counted for the block that shows pillars.
     */
    private static Reference2IntOpenHashMap<Block> currentBlocks = new Reference2IntOpenHashMap<>();
    private static Reference2IntOpenHashMap<Block> lastBlocks = new Reference2IntOpenHashMap<>();
    private static Reference2IntOpenHashMap<Block> currentEmitters = new Reference2IntOpenHashMap<>();
    private static Reference2IntOpenHashMap<Block> lastEmitters = new Reference2IntOpenHashMap<>();
    private static long currentEmitterPosition = NO_POSITION;
    private static long lastEmitterPosition = NO_POSITION;

    @Nullable
    private static ClientLevel queueLevel;
    private static int ticksThisSecond;

    private LightPillarWorldChanges() {
    }

    /**
     * Notes a block state change in a client level, called whenever the old and new state differ.
     */
    public static void onBlockChanged(ClientLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        followLevel(level);
        if (!queue.isTracking()) {
            return;
        }
        currentSecond.blocksChanged++;
        currentBlocks.addTo(newState.isAir() ? oldState.getBlock() : newState.getBlock(), 1);
        boolean oldShowsPillar = LightPillarEmitters.showsPillar(oldState);
        if (oldShowsPillar || LightPillarEmitters.showsPillar(newState)) {
            currentSecond.changesAtLights++;
            currentEmitters.addTo(oldShowsPillar ? oldState.getBlock() : newState.getBlock(), 1);
            currentEmitterPosition = pos.asLong();
        }
        if (queue.onBlockChanged(pos.asLong(), oldState, newState)) {
            currentSecond.sectionsQueuedByBlocks++;
        }
        if (!LightPillarSources.shelter().occluders().stopsTheSame(oldState, newState)) {
            currentSecond.sectionsQueuedByCaps += LightPillarSources.clusters().queueSectionsReadingCapAt(pos.getX(),
                    pos.getY(), pos.getZ(), queue);
        }
        LightPillarSources.shelter().onBlockChanged(pos.getX(), pos.getY(), pos.getZ(), oldState, newState);
    }

    /**
     * Notes light the client computed in a section. The new values are readable when this is called.
     */
    public static void onLightUpdated(ClientLevel level, LightLayer layer, SectionPos section) {
        followLevel(level);
        if (layer != LightLayer.SKY || !queue.isTracking()) {
            return;
        }
        currentSecond.skyLightSectionsComputed++;
        currentSecond.sectionsQueuedByLight += queue.onSkyLightChanged(section.asLong());
    }

    /**
     * Notes light received from the server for a column, in the light sections whose bit is set in either mask.
     * The bits count from the lowest light section. The new values become readable later in the same frame.
     * Every chunk that arrives passes here, so a column waiting for its light is queued whole.
     */
    public static void onLightReceived(ClientLevel level, int chunkX, int chunkZ, LightLayer layer, BitSet dataMask,
            BitSet emptyMask, int lowestLightSection, int lightSectionCount) {
        followLevel(level);
        if (layer != LightLayer.SKY) {
            return;
        }
        boolean wasWaiting = queue.onColumnLit(ChunkPos.asLong(chunkX, chunkZ));
        if (!queue.isTracking()) {
            return;
        }
        if (wasWaiting) {
            currentSecond.columnsLit++;
            currentSecond.sectionsQueuedByCaps += LightPillarSources.clusters().queueSectionsReadingCapsIn(chunkX,
                    chunkZ, queue);
        }
        for (int index = 0; index < lightSectionCount; index++) {
            if (dataMask.get(index) || emptyMask.get(index)) {
                currentSecond.skyLightSectionsReceived++;
                currentSecond.sectionsQueuedByLight += queue.onSkyLightChanged(
                        SectionPos.asLong(chunkX, lowestLightSection + index, chunkZ));
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        followLevel(level);
        ChunkPos chunk = event.getChunk().getPos();
        queue.onColumnLoaded(chunk.toLong());
        if (queue.isTracking()) {
            currentSecond.columnsLoaded++;
        }
        LightPillarSources.shelter().onChunkChanged(chunk.x, chunk.z);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        followLevel(level);
        ChunkPos chunk = event.getChunk().getPos();
        queue.onColumnUnloaded(chunk.toLong());
        if (queue.isTracking()) {
            currentSecond.columnsUnloaded++;
        }
        LightPillarSources.shelter().onChunkChanged(chunk.x, chunk.z);
    }

    /**
     * Forgets the loaded columns of an earlier level when a hook reports a new one.
     */
    private static void followLevel(ClientLevel level) {
        if (level != queueLevel) {
            queue.reset();
            queueLevel = level;
        }
    }

    /**
     * Starts queueing the sections of the level, beginning with every section of the loaded columns that have their
     * sky light.
     */
    static void startTracking(ClientLevel level) {
        followLevel(level);
        queue.startTracking(level.getMinSection(), level.getSectionsCount());
    }

    static void stopTracking() {
        queue.stopTracking();
    }

    static LightPillarWorkQueue queue() {
        return queue;
    }

    /**
     * Rolls the counts over once a second.
     */
    static void tick() {
        ticksThisSecond++;
        if (ticksThisSecond < TICKS_PER_SECOND) {
            return;
        }
        lastSecond.copy(currentSecond);
        currentSecond.clear();
        Reference2IntOpenHashMap<Block> finishedBlocks = currentBlocks;
        currentBlocks = lastBlocks;
        lastBlocks = finishedBlocks;
        currentBlocks.clear();
        Reference2IntOpenHashMap<Block> finishedEmitters = currentEmitters;
        currentEmitters = lastEmitters;
        lastEmitters = finishedEmitters;
        currentEmitters.clear();
        lastEmitterPosition = currentEmitterPosition;
        currentEmitterPosition = NO_POSITION;
        ticksThisSecond = 0;
    }

    /**
     * A line about the world changes of the last full second, for the client command.
     */
    public static String describe() {
        if (!queue.isTracking()) {
            return "World changes are not tracked while the calculated sky is not drawn";
        }
        return String.format(Locale.ROOT,
                "World changes in the last second: %d blocks with %d at lights queuing %d sections and %d more for "
                        + "their caps, sky light in %d sections computed and %d received queuing %d sections, "
                        + "%d chunks loaded, %d lit, %d unloaded; %d chunks loaded in all",
                lastSecond.blocksChanged, lastSecond.changesAtLights, lastSecond.sectionsQueuedByBlocks,
                lastSecond.sectionsQueuedByCaps, lastSecond.skyLightSectionsComputed,
                lastSecond.skyLightSectionsReceived, lastSecond.sectionsQueuedByLight, lastSecond.columnsLoaded,
                lastSecond.columnsLit, lastSecond.columnsUnloaded, queue.loadedColumnCount());
    }

    /**
     * A line about which blocks changed most in the last full second, for the client command.
     */
    public static String describeBlocks() {
        String lastEmitter = lastEmitterPosition == NO_POSITION ? "none"
                : BlockPos.of(lastEmitterPosition).toShortString();
        return "Changed most: " + mostChanged(lastBlocks) + "; at lights: " + mostChanged(lastEmitters)
                + "; last change at a light at " + lastEmitter;
    }

    private static String mostChanged(Reference2IntOpenHashMap<Block> counts) {
        if (counts.isEmpty()) {
            return "none";
        }
        List<Reference2IntMap.Entry<Block>> entries = new ArrayList<>(counts.reference2IntEntrySet());
        entries.sort((first, second) -> Integer.compare(second.getIntValue(), first.getIntValue()));
        StringBuilder text = new StringBuilder();
        for (int rank = 0; rank < Math.min(LISTED_BLOCKS, entries.size()); rank++) {
            if (rank > 0) {
                text.append(", ");
            }
            Reference2IntMap.Entry<Block> entry = entries.get(rank);
            text.append(entry.getIntValue()).append(' ').append(ForgeRegistries.BLOCKS.getKey(entry.getKey()));
        }
        return text.toString();
    }

    /**
     * Counts of hook calls and of the sections they queued.
     */
    private static final class Counts {
        private int blocksChanged;
        private int changesAtLights;
        private int sectionsQueuedByBlocks;
        private int sectionsQueuedByCaps;
        private int skyLightSectionsComputed;
        private int skyLightSectionsReceived;
        private int sectionsQueuedByLight;
        private int columnsLoaded;
        private int columnsLit;
        private int columnsUnloaded;

        private void copy(Counts other) {
            blocksChanged = other.blocksChanged;
            changesAtLights = other.changesAtLights;
            sectionsQueuedByBlocks = other.sectionsQueuedByBlocks;
            sectionsQueuedByCaps = other.sectionsQueuedByCaps;
            skyLightSectionsComputed = other.skyLightSectionsComputed;
            skyLightSectionsReceived = other.skyLightSectionsReceived;
            sectionsQueuedByLight = other.sectionsQueuedByLight;
            columnsLoaded = other.columnsLoaded;
            columnsLit = other.columnsLit;
            columnsUnloaded = other.columnsUnloaded;
        }

        private void clear() {
            blocksChanged = 0;
            changesAtLights = 0;
            sectionsQueuedByBlocks = 0;
            sectionsQueuedByCaps = 0;
            skyLightSectionsComputed = 0;
            skyLightSectionsReceived = 0;
            sectionsQueuedByLight = 0;
            columnsLoaded = 0;
            columnsLit = 0;
            columnsUnloaded = 0;
        }
    }
}
