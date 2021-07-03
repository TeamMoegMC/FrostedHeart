/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataGenerator;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataProvider;
import com.teammoeg.frostedheart.world.chunkdata.IFHChunkGenerator;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;

import java.util.function.Supplier;

@Deprecated
public class FHChunkGenerator /*extends ChunkGenerator implements IFHChunkGenerator*/
{
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