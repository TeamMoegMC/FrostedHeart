/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.online;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirNativeCheckpointProductionGameTests;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.Queue;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts real native loop eligibility without changing the supplied budget or queue. */
@Mixin(value = ChunkMap.class, remap = false)
public abstract class NativeUnloadLoopObservationMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;
    @Shadow @Final private Queue<Runnable> unloadQueue;
    @Inject(method = "processUnloads", at = @At("HEAD"))
    private void nativeLoop$observe(BooleanSupplier haveTime, CallbackInfo ci) {
        ContinuousAirNativeCheckpointProductionGameTests.nativeUnloadLoop(level, haveTime, pendingUnloads, unloadQueue.size());
    }
}
