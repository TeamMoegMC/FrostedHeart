package com.teammoeg.frostedheart.content.trade;

import com.teammoeg.chorda.io.NBTSerializable;

import net.minecraft.nbt.CompoundTag;

public class PlayerTradeData implements NBTSerializable {
	int killed_indirect;
	public PlayerTradeData() {
	}
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		nbt.putInt("killed_indirect", killed_indirect);
		
	}
	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
		killed_indirect=nbt.getInt("killed_indirect");
	}

}
