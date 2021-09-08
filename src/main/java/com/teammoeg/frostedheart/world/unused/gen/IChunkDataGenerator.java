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

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

/**
 * This is the object responsible for generating FH chunk data, in parallel with normal chunk generation.
 * <p>
 * In order to apply this to a custom chunk generator: the chunk generator MUST implement {@link IFHChunkGenerator} and return a {@link ChunkDataProvider}, which contains an instance of this generator.
 */
@Deprecated
public interface IChunkDataGenerator {
   // void generate(ChunkData data, ChunkData.Status status);
}
