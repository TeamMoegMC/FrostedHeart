/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCache;

import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;

/**
 * This is a manager for various cache invalidations, either on resource reload or server start/stop
 */
public enum ChunkCacheInvalidationReloaderListener implements IFutureReloadListener {
    INSTANCE;

    @Override
    public CompletableFuture<Void> reload(IStage stage, IResourceManager resourceManager, IProfiler preparationsProfiler, IProfiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.runAsync(() -> {
        }, backgroundExecutor).thenCompose(stage::markCompleteAwaitingOthers).thenRunAsync(this::invalidateAll, gameExecutor);
    }

    public void invalidateAll() {
        ChunkDataCache.clearAll();
    }
}