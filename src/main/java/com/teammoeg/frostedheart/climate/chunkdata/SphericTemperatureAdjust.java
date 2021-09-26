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

package com.teammoeg.frostedheart.climate.chunkdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

/**
 * Spheric Temperature Adjust, would adjust temperature in a ball.
 */
public class SphericTemperatureAdjust extends CubicTemperatureAdjust {
    public SphericTemperatureAdjust(BlockPos heatPos, int range, byte tempMod) {
        super(heatPos, range, tempMod);
    }

    long r2;

    public SphericTemperatureAdjust(int cx, int cy, int cz, int r, byte value) {
        super(cx, cy, cz, r, value);
        r2 = r * r;
    }

    public SphericTemperatureAdjust(PacketBuffer buffer) {
        super(buffer);
        r2 = r * r;
    }

    public SphericTemperatureAdjust(CompoundNBT nc) {
        super(nc);
        r2 = r * r;
    }

    @Override
    public boolean isEffective(int x, int y, int z) {
        long l = (long) Math.pow(x - cx, 2);
        l += (long) Math.pow(y - cy, 2);
        l += (long) Math.pow(z - cz, 2);
        return l <= r;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = serializeNBTData();
        nbt.putInt("type", 2);
        return nbt;
    }

    @Override
    public void serialize(PacketBuffer buffer) {
        buffer.writeInt(2);
        super.serializeData(buffer);
    }

}
