package com.teammoeg.frostedheart.climate.chunkdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;

public interface ITemperatureAdjust extends INBTSerializable<CompoundNBT> {
	byte getTemperatureAt(int x,int y,int z);
	default byte getTemperatureAt(BlockPos bp) {
		return getTemperatureAt(bp.getX(),bp.getY(),bp.getZ());
	};
	
	boolean isEffective(int x,int y,int z);
	default boolean isEffective(BlockPos bp) {
		return isEffective(bp.getX(),bp.getY(),bp.getZ());
	}
	void serialize(PacketBuffer buffer);
	void deserialize(PacketBuffer buffer);
	public static ITemperatureAdjust valueOf(PacketBuffer buffer) {
		int packetId=buffer.readVarInt();
		switch(packetId) {
		case 1:new CubicTemperatureAdjust(buffer);
		case 2:return new SphericTemperatureAdjust(buffer);
		default:return new CubicTemperatureAdjust(buffer);
		}
	}
	static ITemperatureAdjust valueOf(CompoundNBT nc) {
		switch(nc.getInt("type")) {
		case 1:new CubicTemperatureAdjust(nc);
		case 2:return new SphericTemperatureAdjust(nc);
		default:return new CubicTemperatureAdjust(nc);
		}
	}
	int getCenterX();

	int getCenterY();

	int getCenterZ();
	float getValueAt(BlockPos pos);
}
