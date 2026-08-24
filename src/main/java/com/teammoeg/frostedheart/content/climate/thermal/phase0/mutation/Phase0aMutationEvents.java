/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Phase0aMutationEvents {
    private static final AtomicLong NEXT_LIFECYCLE_SEQUENCE = new AtomicLong();
    private static final Map<LifecycleKey, MutableLifecycleObservation> LIFECYCLE_OBSERVATIONS =
            new HashMap<>();

    private Phase0aMutationEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (Phase0aMutationProbe.isEnabled()
                && event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof LevelChunk chunk) {
            observeLifecycle(level, chunk, LifecycleStage.LOAD_BEFORE);
            try {
                Phase0aMutationProbe.onChunkLoad(level, chunk);
            } finally {
                observeLifecycle(level, chunk, LifecycleStage.LOAD_AFTER);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (Phase0aMutationProbe.isEnabled()
                && event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof LevelChunk chunk) {
            observeLifecycle(level, chunk, LifecycleStage.UNLOAD_BEFORE);
            try {
                Phase0aMutationProbe.onChunkUnload(level, chunk);
            } finally {
                observeLifecycle(level, chunk, LifecycleStage.UNLOAD_AFTER);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (Phase0aMutationProbe.isEnabled()
                && event.side == LogicalSide.SERVER
                && event.phase == TickEvent.Phase.END
                && event.level instanceof ServerLevel level) {
            Phase0aMutationProbe.sealLevel(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (Phase0aMutationProbe.isEnabled()) {
            Phase0aMutationProbe.clearAfterServerStop();
            synchronized (LIFECYCLE_OBSERVATIONS) {
                LIFECYCLE_OBSERVATIONS.clear();
            }
        }
    }

    static LifecycleObservation lifecycleObservation(ServerLevel level, ChunkPos chunkPos) {
        LifecycleKey key = new LifecycleKey(level.dimension(), chunkPos.toLong());
        synchronized (LIFECYCLE_OBSERVATIONS) {
            MutableLifecycleObservation observation = LIFECYCLE_OBSERVATIONS.get(key);
            return observation == null ? LifecycleObservation.EMPTY : observation.snapshot();
        }
    }

    private static void observeLifecycle(ServerLevel level, LevelChunk chunk, LifecycleStage stage) {
        LifecycleKey key = new LifecycleKey(level.dimension(), chunk.getPos().toLong());
        long sequence = NEXT_LIFECYCLE_SEQUENCE.incrementAndGet();
        int chunkIdentity = System.identityHashCode(chunk);
        synchronized (LIFECYCLE_OBSERVATIONS) {
            MutableLifecycleObservation observation = LIFECYCLE_OBSERVATIONS.computeIfAbsent(
                    key, ignored -> new MutableLifecycleObservation());
            observation.record(stage, sequence, chunkIdentity);
        }
    }

    enum LifecycleStage {
        LOAD_BEFORE,
        LOAD_AFTER,
        UNLOAD_BEFORE,
        UNLOAD_AFTER
    }

    record LifecycleObservation(
            long loadBeforeSequence,
            long loadAfterSequence,
            long unloadBeforeSequence,
            long unloadAfterSequence,
            int loadedChunkIdentity,
            int unloadedChunkIdentity) {
        private static final LifecycleObservation EMPTY = new LifecycleObservation(0L, 0L, 0L, 0L, 0, 0);
    }

    private record LifecycleKey(ResourceKey<Level> dimension, long chunkPos) {
    }

    private static final class MutableLifecycleObservation {
        private long loadBeforeSequence;
        private long loadAfterSequence;
        private long unloadBeforeSequence;
        private long unloadAfterSequence;
        private int loadedChunkIdentity;
        private int unloadedChunkIdentity;

        private void record(LifecycleStage stage, long sequence, int chunkIdentity) {
            switch (stage) {
                case LOAD_BEFORE -> {
                    loadBeforeSequence = sequence;
                    loadedChunkIdentity = chunkIdentity;
                }
                case LOAD_AFTER -> loadAfterSequence = sequence;
                case UNLOAD_BEFORE -> {
                    unloadBeforeSequence = sequence;
                    unloadedChunkIdentity = chunkIdentity;
                }
                case UNLOAD_AFTER -> unloadAfterSequence = sequence;
            }
        }

        private LifecycleObservation snapshot() {
            return new LifecycleObservation(
                    loadBeforeSequence,
                    loadAfterSequence,
                    unloadBeforeSequence,
                    unloadAfterSequence,
                    loadedChunkIdentity,
                    unloadedChunkIdentity);
        }
    }
}
