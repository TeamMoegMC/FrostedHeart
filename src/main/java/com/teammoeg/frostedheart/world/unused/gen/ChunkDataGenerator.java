/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.unused.gen;

import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import com.teammoeg.frostedheart.world.noise.INoise1D;
import com.teammoeg.frostedheart.world.noise.INoise2D;
import com.teammoeg.frostedheart.world.noise.OpenSimplex2D;
import net.minecraft.util.math.ChunkPos;

import java.util.Random;

/**
 * This is FH's default chunk data generator.
 * If you want to use a vanilla or custom chunk generator, you can use this chunk data generator, or attach your own.
 *
 * @see IChunkDataGenerator
 */
@Deprecated
public class ChunkDataGenerator implements IChunkDataGenerator {

    /**
     * Constants for temperature calculation. Do not reference these directly, they do not have much meaning outside the context they are used in
     */
    public static final float MINIMUM_TEMPERATURE_SCALE = -24f;
    public static final float MAXIMUM_TEMPERATURE_SCALE = 30f;
    public static final float LATITUDE_TEMPERATURE_VARIANCE_AMPLITUDE = -6.5f;
    public static final float LATITUDE_TEMPERATURE_VARIANCE_MEAN = 13.5f;
    public static final float REGIONAL_TEMPERATURE_SCALE = 2f;
    public static final float REGIONAL_RAINFALL_SCALE = 50f;

    private final INoise2D temperatureNoise;

    public ChunkDataGenerator(long worldSeed, Random seedGenerator) {
        // Climate
        temperatureNoise = INoise1D.triangle(1, 0, 1f / (2f * 20000), 0)
                .extendX()
                .scaled(MINIMUM_TEMPERATURE_SCALE, MAXIMUM_TEMPERATURE_SCALE)
                .add(new OpenSimplex2D(seedGenerator.nextLong())
                        .octaves(2)
                        .spread(12f / 20000)
                        .scaled(-REGIONAL_TEMPERATURE_SCALE, REGIONAL_TEMPERATURE_SCALE));
    }

    @Override
    public void generate(ChunkData data, ChunkData.Status status) {
        ChunkPos pos = data.getPos();
        int chunkX = pos.getXStart(), chunkZ = pos.getZStart();
        switch (status) {
            case EMPTY:
            case CLIENT:
                throw new IllegalStateException("Should not ever generate EMPTY or CLIENT status!");
            case CLIMATE:
                generateClimate(data, chunkX, chunkZ);
                break;
        }
    }

    private void generateClimate(ChunkData data, int chunkX, int chunkZ) {
        data.initChunkMatrix((byte) 10);
    }
}
