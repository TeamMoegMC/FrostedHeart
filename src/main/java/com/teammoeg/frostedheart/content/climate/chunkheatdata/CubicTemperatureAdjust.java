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

package com.teammoeg.frostedheart.content.climate.chunkheatdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * Cubic Temperature Adjust, would adjust temperature in a cube
 */
public class CubicTemperatureAdjust implements ITemperatureAdjust {
    int cx;
    int cy;
    int cz;
    int r;
    int value;

    public CubicTemperatureAdjust(BlockPos heatPos, int range, int tempMod) {
        this(heatPos.getX(), heatPos.getY(), heatPos.getZ(), range, tempMod);
    }

    public CubicTemperatureAdjust(CompoundNBT nc) {
        deserializeNBT(nc);
    }

    public CubicTemperatureAdjust(int cx, int cy, int cz, int r, int value) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.r = r;
        this.value = value;
    }

    public CubicTemperatureAdjust(PacketBuffer buffer) {
        deserialize(buffer);
    }


    @Override
    public void deserialize(PacketBuffer buffer) {
        cx = buffer.readVarInt();
        cy = buffer.readVarInt();
        cz = buffer.readVarInt();
        r = buffer.readVarInt();
        value = buffer.readByte();
    }

    public int getCenterX() {
        return cx;
    }

    public int getCenterY() {
        return cy;
    }

    public int getCenterZ() {
        return cz;
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
        if (Math.abs(x - cx) <= r && Math.abs(y - cy) <= r && Math.abs(z - cz) <= r)
            return true;
        return false;
    }

    @Override
    public void serialize(PacketBuffer buffer) {
        buffer.writeByte(1);//packet id
        serializeData(buffer);
    }

    protected void serializeData(PacketBuffer buffer) {
        buffer.writeVarInt(cx);
        buffer.writeVarInt(cy);
        buffer.writeVarInt(cz);
        buffer.writeVarInt(r);
        buffer.writeByte(value);
    }



    @Override
    public void setValue(int value) {
        this.value = value;
    }

	@Override
	public void save(CompoundNBT nbt, boolean isPacket) {
		BlockPos pos=new BlockPos(cx,cy,cz);
		nbt.putLong("location", pos.toLong());
        nbt.putInt("range", r);
        nbt.putInt("value", value);
	}

	@Override
	public void load(CompoundNBT nbt, boolean isPacket) {
		if(nbt.contains("location", Constants.NBT.TAG_INT_ARRAY)) {
			int[] loc = nbt.getIntArray("location");
	        cx = loc[0];
	        cy = loc[1];
	        cz = loc[2];
		}else {
			BlockPos bp=BlockPos.fromLong(nbt.getLong("location"));
			cx=bp.getX();
			cy=bp.getY();
			cz=bp.getZ();
		}
        
        r = nbt.getInt("range");
        value = nbt.getInt("value");
	}

}
