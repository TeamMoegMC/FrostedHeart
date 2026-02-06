/*
 * Copyright (c) 2026 TeamMoeg
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
 *
 */

package com.teammoeg.frostedheart.content.climate.gamedata.chunkheat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;


public class SphereHeatArea extends CubicHeatArea {
    public static Codec<SphereHeatArea> CODEC = RecordCodecBuilder.create(t -> t.group(BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.center),
            Codec.INT.fieldOf("r").forGetter(o -> o.r),
            Codec.INT.fieldOf("v").forGetter(o -> o.value)).apply(t, SphereHeatArea::new));

    public SphereHeatArea(BlockPos center, int range, int tempMod) {
        super(center, range, tempMod);
    }

    @Override
    public int getTemperatureAt(int x, int y, int z) {
        if (isEffective(x, y, z))
            return value;
        return 0;
    }

    @Override
    public float[] getStructData() {
        return new float[] {center.getX() + 0.5f, center.getY() + 0.5f, center.getZ() + 0.5f, 2, value-20, getRadius()+0.005f, 0, 0};
    }

    @Override
    public float getValueAt(BlockPos pos) {
        return value;
    }

    @Override
    public boolean isEffective(int x, int y, int z) {
        long l = (long) Math.pow(x - getCenterX(), 2);
        l += (long) Math.pow(y - getCenterY(), 2);
        l += (long) Math.pow(z - getCenterZ(), 2);
        return l <= (long) r * r;
    }

    @Override
    public BlockPos getCenter() {
        return center;
    }


    @Override
    public String toString() {
        return "SphereHeatArea [center=" + center + ", r=" + r + ", value=" + value + "]";
    }

}
