/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.unused.gen;

import net.minecraft.world.gen.ChunkGenerator;

/**
 * Identifier interface for the FH enabled chunk generators
 * <p>
 * Any custom chunk generator wishing to use features from FH MUST implement this and return a valid chunk data provider
 * This is also used in various places (such as spawn position placement) to identify FH world generators
 */
@Deprecated
public interface IFHChunkGenerator {
    /**
     * @return The chunk data provider for this generator.
     */
    ChunkDataProvider getChunkDataProvider();

    default ChunkGenerator chunkGenerator() {
        return (ChunkGenerator) this;
    }
}