/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.thermal;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin_ThermalMaintenanceSave {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;

    @Shadow
    private boolean save(ChunkAccess chunk) {
        throw new AssertionError("Mixin shadow");
    }

    @Shadow
    private void scheduleUnload(long position, ChunkHolder holder) {
        throw new AssertionError("Mixin shadow");
    }


    @Redirect(method = "scheduleUnload", at = @At(value = "INVOKE", target =
            "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> frostedheart$prepareThermalUnload(
            CompletableFuture<ChunkAccess> chunkReady, Consumer<ChunkAccess> unload, Executor unloadQueue,
            long position, ChunkHolder holder) {
        if (!MinecraftThermalInput.usesNativeCheckpointScheduling(level))
            return chunkReady.thenAcceptAsync(unload, unloadQueue);
        // Keep the original future captured by vanilla's closure. It still checks
        // that future and pending holder before it retires or writes the chunk.
        BooleanSupplier stillPending = () -> pendingUnloads.get(position) == holder
                && holder.getChunkToSave() == chunkReady;
        return chunkReady.thenComposeAsync(chunk -> MinecraftThermalInput.prepareNativeUnload(
                level, chunk, unloadQueue, stillPending), unloadQueue).thenAcceptAsync(checkpoint -> {
            if (!checkpoint.run(unload)) scheduleUnload(position, holder);
        }, unloadQueue);
    }

    // Both optional maintenance and non-flush autosave use this entry. Explicit
    // flush and actual unload writes call save directly and keep their own fences.
    @Redirect(method = "saveChunkIfNeeded", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ChunkMap;save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z"))
    private boolean frostedheart$prepareMaintenanceSave(ChunkMap chunkMap, ChunkAccess chunk) {
        if (!chunk.isUnsaved()) return save(chunk);
        LevelChunk fullChunk = chunk instanceof LevelChunk full ? full
                : chunk instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : null;
        boolean saved = MinecraftThermalInput.runMaintenanceSave(level, fullChunk, () -> save(chunk));
        if (saved && fullChunk != null) MinecraftThermalInput.onMaintenanceSave(level, fullChunk);
        return saved;
    }
}
