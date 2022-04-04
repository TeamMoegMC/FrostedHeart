package com.teammoeg.frostedheart.research;

import net.minecraft.nbt.CompoundNBT;

public class UnlockList {
	CompoundNBT nbt;

	public UnlockList() {
		nbt=new CompoundNBT();
	}
	public UnlockList(CompoundNBT nbt) {
		this.nbt=nbt;
	}

	public boolean has(String key) {
		return nbt.getBoolean(key);
	}
	public CompoundNBT serialize() {
		return nbt;
	}
}
