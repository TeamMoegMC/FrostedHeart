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

package com.teammoeg.frostedheart.world.unused.gen;

@Deprecated
public class FHChunkGenerator /*extends ChunkGenerator implements IFHChunkGenerator*/ {
//    public static final Codec<FHChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
//        BiomeProvider.CODEC.fieldOf("biome_source").forGetter(c -> c.biomeProvider),
//        DimensionSettings.CODEC.fieldOf("settings").forGetter(c -> () -> c.settings),
//        Codec.LONG.fieldOf("seed").forGetter(c -> c.seed)
//    ).apply(instance, FHChunkGenerator::new));
//
//    private final ChunkDataProvider chunkDataProvider;
//
//    // Properties set from codec
//    private final DimensionSettings settings;
//    private final long seed;
//
//    public FHChunkGenerator(BiomeProvider biomeProvider, Supplier<DimensionSettings> settings, long seed)
//    {
//        super(biomeProvider, settings.get().getStructures());
//
//        this.settings = settings.get();
//        this.seed = seed;
//
//        final SharedSeedRandom seedGenerator = new SharedSeedRandom(seed);
//
//        // Generators / Providers
//        this.chunkDataProvider = new ChunkDataProvider(new ChunkDataGenerator(seed, seedGenerator, this.biomeProvider.getLayerSettings())); // Chunk data
//    }
//
//    @Override
//    public ChunkDataProvider getChunkDataProvider()
//    {
//        return chunkDataProvider;
//    }
//
//    @Override
//    protected Codec<FHChunkGenerator> func_230347_a_()
//    {
//        return CODEC;
//    }
//
//    @Override
//    public ChunkGenerator func_230349_a_(long seedIn)
//    {
//        return new FHChunkGenerator(biomeProvider, () -> settings, seedIn);
//    }
}