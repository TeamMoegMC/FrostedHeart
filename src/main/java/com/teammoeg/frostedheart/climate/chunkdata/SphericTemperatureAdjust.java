package com.teammoeg.frostedheart.climate.chunkdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class SphericTemperatureAdjust extends CubicTemperatureAdjust {
	long r2;
	public SphericTemperatureAdjust(int cx, int cy, int cz, int r, byte value) {
		super(cx, cy, cz, r, value);
		r2=r*r;
	}
	public SphericTemperatureAdjust(PacketBuffer buffer) {
		super(buffer);
		r2=r*r;
	}
	public SphericTemperatureAdjust(CompoundNBT nc) {
		super(nc);
		r2=r*r;
	}
	@Override
	public boolean isEffective(int x, int y, int z) {
		long l=(long) Math.pow(x-cx,2);
		l+=(long) Math.pow(y-cy,2);
		l+=(long) Math.pow(z-cz,2);
		return l<=r;
	}

	@Override
	public CompoundNBT serializeNBT() {
		CompoundNBT nbt = serializeNBTData() ;
		nbt.putInt("type",2);
		return nbt;
	}
	@Override
	public void serialize(PacketBuffer buffer) {
		buffer.writeInt(2);
		super.serializeData(buffer);
	}

}
