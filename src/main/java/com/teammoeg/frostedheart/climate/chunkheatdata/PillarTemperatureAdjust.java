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

package com.teammoeg.frostedheart.climate.chunkheatdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

/**
 * Pillar Temperature Adjust, would adjust temperature in a pillar.
 */
public class PillarTemperatureAdjust extends CubicTemperatureAdjust {

    long r2;
    int upper;
    int lower;

    public PillarTemperatureAdjust(BlockPos heatPos, int range, int u, int d, int tempMod) {
        super(heatPos, range, tempMod);
        r2 = r * r;
        this.upper = u;
        this.lower = d;
    }

    public PillarTemperatureAdjust(CompoundNBT nc) {
        super(nc);
        r2 = r * r;
        this.upper = nc.getInt("upper");
        this.lower = nc.getInt("lower");
    }

    public PillarTemperatureAdjust(int cx, int cy, int cz, int r, int upper, int lower, int value) {
        super(cx, cy, cz, r, value);
        r2 = r * r;
        this.upper = upper;
        this.lower = lower;
    }

    public PillarTemperatureAdjust(PacketBuffer buffer) {
        super(buffer);
        r2 = r * r;
        this.upper = buffer.readVarInt();
        this.lower = buffer.readVarInt();
    }

    @Override
    public boolean isEffective(int x, int y, int z) {
        if (y > upper + cy || y < cy - lower) return false;
        long l = (long) Math.pow(x - cx + 0.5, 2);
        l += (long) Math.pow(z - cz + 0.5, 2);
        return l <= r2;
    }

    @Override
    public void serialize(PacketBuffer buffer) {
        buffer.writeByte(2);
        serializeData(buffer);
    }

    @Override
    protected void serializeData(PacketBuffer buffer) {
        super.serializeData(buffer);
        buffer.writeVarInt(upper);
        buffer.writeVarInt(lower);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = serializeNBTData();
        nbt.putInt("type", 2);
        nbt.putInt("upper", upper);
        nbt.putInt("lower", lower);
        return nbt;
    }

}
