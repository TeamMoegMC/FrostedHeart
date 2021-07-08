/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.listener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCache;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;

/**
 * This is a manager for various cache invalidations, either on resource reload or server start/stop
 */
public enum ChunkCacheInvalidationReloaderListener implements IFutureReloadListener
{
    INSTANCE;

    @Override
    public CompletableFuture<Void> reload(IStage stage, IResourceManager resourceManager, IProfiler preparationsProfiler, IProfiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor)
    {
        return CompletableFuture.runAsync(() -> {}, backgroundExecutor).thenCompose(stage::markCompleteAwaitingOthers).thenRunAsync(this::invalidateAll, gameExecutor);
    }

    public void invalidateAll()
    {
        ChunkDataCache.clearAll();
    }
}