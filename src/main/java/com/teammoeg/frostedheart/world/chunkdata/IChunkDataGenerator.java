/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.chunkdata;

/**
 * This is the object responsible for generating FH chunk data, in parallel with normal chunk generation.
 *
 * In order to apply this to a custom chunk generator: the chunk generator MUST implement {@link IFHChunkGenerator} and return a {@link ChunkDataProvider}, which contains an instance of this generator.
 */
@Deprecated
public interface IChunkDataGenerator
{
    void generate(ChunkData data, ChunkData.Status status);
}
