package com.teammoeg.frostedheart.climate.chunkdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class CubicTemperatureAdjust implements ITemperatureAdjust {
	int cx;
	int cy;
	int cz;
	public CubicTemperatureAdjust(int cx, int cy, int cz, int r, byte value) {
		this.cx = cx;
		this.cy = cy;
		this.cz = cz;
		this.r = r;
		this.value = value;
	}

	public CubicTemperatureAdjust(PacketBuffer buffer) {
		deserialize(buffer);
	}

	public CubicTemperatureAdjust(CompoundNBT nc) {
		deserializeNBT(nc);
	}

	int r;
	byte value;
	@Override
	public CompoundNBT serializeNBT() {
		CompoundNBT nbt = serializeNBTData() ;
		nbt.putInt("type",1);
		return nbt;
	}

	protected CompoundNBT serializeNBTData() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putIntArray("location",new int[] {cx,cy,cz});
		nbt.putInt("range",r);
		nbt.putByte("value", value);
		return nbt;
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

	public byte getValue() {
		return value;
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt) {
		int[] loc=nbt.getIntArray("location");
		cx=loc[0];
		cy=loc[1];
		cz=loc[2];
		r=nbt.getInt("range");
		value=nbt.getByte("value");
	}

	@Override
	public byte getTemperatureAt(int x, int y, int z) {
		if(isEffective(x,y,z))
			return value;
		return 0;
	}

	@Override
	public boolean isEffective(int x, int y, int z) {
		if(Math.abs(x-cx)<=r&&Math.abs(y-cy)<=r&&Math.abs(z-cz)<=r)
			return true;
		return false;
	}

	@Override
	public void serialize(PacketBuffer buffer) {
		buffer.writeVarInt(1);//packet id
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
	public void deserialize(PacketBuffer buffer) {
		cx=buffer.readVarInt();
		cy=buffer.readVarInt();
		cz=buffer.readVarInt();
		r=buffer.readVarInt();
		value=buffer.readByte();
	}

	@Override
	public float getValueAt(BlockPos pos) {
		return value;
	}

}
