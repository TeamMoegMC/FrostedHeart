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