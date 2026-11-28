/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;

/**
 * 一个 Section 里逐格的自然温度：每个 4³ Brick 的维度与群系温度、每格的覆盖厚度、该区块各覆盖厚度下到达的气候。
 * 主线程构建后不再修改，worker 只读。
 * <p>
 * Per-block natural temperature of one Section: the dimension plus biome temperature of each 4³ Brick, the cover of
 * each block, and the climate reaching each cover in this chunk. Built on the main thread, never mutated, read by the
 * worker. {@link #at} reproduces {@code WorldTemperature.naturalAir} at the block.
 *
 * @param brickBase       dimension plus biome temperature of each Brick, in Brick order
 * @param cover           opaque full blocks above each block, in Page block order
 * @param coveredClimate  climate reaching each cover, index = cover
 * @param minimumY        lowest world Y of the Section
 * @param noUnderground   true in a dimension without an underground (superflat)
 * @param absoluteZeroC   lower clamp of natural temperature
 */
public record NaturalProfile(float[] brickBase, byte[] cover, float[] coveredClimate, int minimumY,
        boolean noUnderground, float absoluteZeroC) {
    public NaturalProfile {
        if (brickBase.length != 64 || cover.length != 4096 || coveredClimate.length == 0)
            throw new IllegalArgumentException("natural profile shape is invalid");
    }

    /** Natural temperature of the Page block {@code (x & 15) | (z & 15) << 4 | (y & 15) << 8}. */
    public double at(int pageBlock) {
        int x = pageBlock & 15, z = pageBlock >>> 4 & 15, y = pageBlock >>> 8;
        int brick = x >>> 2 | (z >>> 2) << 2 | (y >>> 2) << 4;
        int index = Math.min(Byte.toUnsignedInt(cover[pageBlock]), coveredClimate.length - 1);
        double natural = brickBase[brick] + WorldTemperature.altitude(minimumY + y, noUnderground) + coveredClimate[index];
        return Math.max(absoluteZeroC, natural);
    }

    /** The same profile under a new covered-climate table; geometry-derived arrays are shared. */
    public NaturalProfile withClimate(float[] climate) {
        return new NaturalProfile(brickBase, cover, climate, minimumY, noUnderground, absoluteZeroC);
    }

    /** The same profile with new cover; the caller hands over the array. */
    public NaturalProfile withCover(byte[] nextCover) {
        return new NaturalProfile(brickBase, nextCover, coveredClimate, minimumY, noUnderground, absoluteZeroC);
    }
}
