/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Phase0aMutationEvents {
    private Phase0aMutationEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (Phase0aMutationProbe.isEnabled()
                && event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof LevelChunk chunk) {
            Phase0aMutationProbe.onChunkLoad(level, chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (Phase0aMutationProbe.isEnabled()
                && event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof LevelChunk chunk) {
            Phase0aMutationProbe.onChunkUnload(level, chunk);
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
        }
    }
}
