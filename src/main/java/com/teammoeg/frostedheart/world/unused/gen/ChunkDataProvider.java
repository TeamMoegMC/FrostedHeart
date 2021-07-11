/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.unused.gen;

import com.google.common.annotations.VisibleForTesting;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/**
 * This acts as a bridge between the {@link ChunkGenerator}, FH's chunk data caches and tracking, and the {@link IChunkDataGenerator}.
 * In order to customize the chunk data generation, see {@link IChunkDataGenerator}
 */
@Deprecated
public final class ChunkDataProvider
{
    /**
     * Directly tries to access the chunk data provider through the overworld.
     */
    public static ChunkDataProvider getOrThrow()
    {
        return getOrThrow(ServerLifecycleHooks.getCurrentServer().func_241755_D_());
    }

    public static ChunkDataProvider getOrThrow(IWorld world)
    {
        AbstractChunkProvider chunkProvider = world.getChunkProvider();
        if (chunkProvider instanceof ServerChunkProvider)
        {
            return getOrThrow(((ServerChunkProvider) chunkProvider).getChunkGenerator());
        }
        throw new IllegalStateException("Tried to access ChunkDataProvider but no ServerChunkProvider was found on world: " + world);
    }

    /**
     * Tries to access the chunk data provider through the chunk generator, mostly used during feature generation when we have direct access to the generator.
     */
    public static ChunkDataProvider getOrThrow(ChunkGenerator chunkGenerator)
    {
        if (chunkGenerator instanceof IFHChunkGenerator)
        {
            return ((IFHChunkGenerator) chunkGenerator).getChunkDataProvider();
        }
        throw new IllegalStateException("Tried to access ChunkDataProvider but none was present on " + chunkGenerator);
    }

    private final IChunkDataGenerator generator;

    public ChunkDataProvider(IChunkDataGenerator generator)
    {
        this.generator = generator;
    }

    /**
     * Gets the chunk data for a chunk, during world generation.
     * The default implementation generates chunk data using FH semantics, and stores generated data in {@link ChunkDataCache#WORLD_GEN}
     * Implementors are free to return any form of data.
     *
     * @param pos            The chunk position
     * @param requiredStatus The minimum status of the chunk data returned
     * @return A chunk data for the provided chunk pos
     */
    public final ChunkData get(BlockPos pos, ChunkData.Status requiredStatus)
    {
        return get(new ChunkPos(pos), requiredStatus);
    }

    /**
     * Gets the chunk data for a chunk, during world generation.
     * The default implementation generates chunk data using FH semantics, and stores generated data in {@link ChunkDataCache#WORLD_GEN}
     * Implementors are free to return any form of data.
     *
     * @param pos            The chunk position
     * @param requiredStatus The minimum status of the chunk data returned
     * @return A chunk data for the provided chunk pos
     */
    public final ChunkData get(ChunkPos pos, ChunkData.Status requiredStatus)
    {
        final ChunkData data = ChunkDataCache.WORLD_GEN.getOrCreate(pos);
        while (!data.getStatus().isAtLeast(requiredStatus))
        {
            final ChunkData.Status next = data.getStatus().next();
            generator.generate(data, next);
            data.setStatus(next);
        }
        return data;
    }

    @VisibleForTesting
    public IChunkDataGenerator getGenerator()
    {
        return generator;
    }

    @Override
    public String toString()
    {
        return "ChunkDataProvider[" + generator.getClass().getSimpleName() + ']';
    }
}