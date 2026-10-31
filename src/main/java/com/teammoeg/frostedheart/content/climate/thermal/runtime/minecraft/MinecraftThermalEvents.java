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
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;

/** Forge lifecycle entry points used by the production thermal runtime. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MinecraftThermalEvents {
    private MinecraftThermalEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ThermalWorkerPool.startShared();
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) MinecraftThermalInput.bootstrapLoadedSources(server);
    }

    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load event) {
        var stageObserver = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        boolean observeStage = stageObserver != null;
        if (observeStage) stageObserver.stage("thermal_data_load", true);
        try {
            if (event.getStatus() != ChunkStatus.ChunkType.LEVELCHUNK
                    || !(event.getChunk() instanceof LevelChunk chunk)) {
                return;
            }
            MinecraftThermalInput.setDormantState(
                    chunk,
                    DormantChunkThermalState.decode(
                            event.getData(),
                            chunk.getSectionYFromSectionIndex(0),
                            chunk.getSections().length));
        } finally { if (observeStage) stageObserver.stage("thermal_data_load", false); }

    }

    @SubscribeEvent
    public static void onChunkDataSave(ChunkDataEvent.Save event) {
        var stageObserver = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        boolean observeStage = stageObserver != null;
        if (observeStage) stageObserver.stage("thermal_data_save", true);
        try {
            LevelChunk chunk = fullChunk(event.getChunk());
            if (chunk == null) {
                return;
            }
            if (event.getLevel() instanceof ServerLevel level) {
                MinecraftThermalInput.checkpointForSave(level, chunk);
            }
            DormantChunkThermalState state = MinecraftThermalInput.dormantState(chunk);
            if (state == null) {
                event.getData().remove("FrostedHeartThermal");
            } else {
                state.encode(event.getData());
            }
        } finally { if (observeStage) stageObserver.stage("thermal_data_save", false); }

    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        var stageObserver = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        boolean observeStage = stageObserver != null;
        if (observeStage) stageObserver.stage("thermal_chunk_load", true);
        try {
            if (event.getLevel() instanceof ServerLevel level
                    && event.getChunk() instanceof LevelChunk chunk) {
                MinecraftThermalInput.onChunkLoad(level, chunk);
            }
        } finally { if (observeStage) stageObserver.stage("thermal_chunk_load", false); }

    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        var stageObserver = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        boolean observeStage = stageObserver != null;
        if (observeStage) stageObserver.stage("thermal_chunk_unload", true);
        try {
            if (event.getLevel() instanceof ServerLevel level
                    && event.getChunk() instanceof LevelChunk chunk) {
                MinecraftThermalInput.onChunkUnload(level, chunk);
            }
        } finally { if (observeStage) stageObserver.stage("thermal_chunk_unload", false); }

    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            MinecraftThermalInput.closeActiveLevel(level);
            MinecraftGameplayFields.unload(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            MinecraftThermalInput.onPlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && player.getServer() != null) {
            ServerLevel previous = player.getServer().getLevel(event.getFrom());
            if (previous != null) {
                MinecraftThermalInput.onPlayerChangedDimension(player, previous);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER
                && event.phase == TickEvent.Phase.END
                && event.level instanceof ServerLevel level) {
            MinecraftThermalInput.tickLevel(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftThermalInput.checkpointAllForStop();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MinecraftThermalInput.closeAll();
        MinecraftGameplayFields.stop();
    }

    private static LevelChunk fullChunk(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        if (chunk instanceof LevelChunk levelChunk) {
            return levelChunk;
        }
        return chunk instanceof ImposterProtoChunk imposter
                ? imposter.getWrapped() : null;
    }
}
