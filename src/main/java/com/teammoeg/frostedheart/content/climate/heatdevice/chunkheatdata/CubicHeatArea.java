/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.util.math.BlockPos;

/**
 * Cubic Temperature Adjust, would adjust temperature in a cube
 */
public class CubicHeatArea implements IHeatArea {
	public static Codec<CubicHeatArea> CODEC=RecordCodecBuilder.create(t->t.group(CodecUtil.BLOCKPOS.fieldOf("pos").forGetter(o->o.center),
		Codec.INT.fieldOf("r").forGetter(o->o.r),
		Codec.INT.fieldOf("v").forGetter(o->o.value)).apply(t,CubicHeatArea::new));
    BlockPos center;
    int r;
    int value;


    public CubicHeatArea(BlockPos center, int range, int tempMod) {
        this.center = center;
        this.r = range;
        this.value = tempMod;
    }


    public int getCenterX() {
        return center.getX();
    }

    public int getCenterY() {
        return center.getY();
    }

    public int getCenterZ() {
        return center.getZ();
    }

    public int getRadius() {
        return r;
    }

    @Override
    public int getTemperatureAt(int x, int y, int z) {
        if (isEffective(x, y, z))
            return value;
        return 0;
    }

    public int getValue() {
        return value;
    }

    @Override
    public float getValueAt(BlockPos pos) {
        return value;
    }

    @Override
    public boolean isEffective(int x, int y, int z) {
        return Math.abs(x - getCenterX()) <= r && Math.abs(y - getCenterY()) <= r && Math.abs(z - getCenterZ()) <= r;
    }



    @Override
    public void setValue(int value) {
        this.value = value;
    }


	@Override
	public BlockPos getCenter() {
		// TODO Auto-generated method stub
		return center;
	}


	@Override
	public String toString() {
		return "CubicHeatArea [center=" + center + ", r=" + r + ", value=" + value + "]";
	}

}
