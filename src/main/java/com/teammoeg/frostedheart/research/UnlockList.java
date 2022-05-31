package com.teammoeg.frostedheart.research;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

public class UnlockList {
	CompoundNBT nbt;

	public UnlockList() {
		nbt=new CompoundNBT();
	}
	public UnlockList(CompoundNBT nbt) {
		this.nbt=nbt;
	}
	public boolean has(ResourceLocation key) {
		return has(key.toString());
	}
	public boolean has(String key) {
		return nbt.contains(key);
	}
	public void unlock(ResourceLocation key) {
		unlock(key.toString());
	}
	public void unlock(String key) {
		nbt.putBoolean(key,true);
	}
	public CompoundNBT serialize() {
		return nbt;
	}
	public void lock(String key) {
		nbt.remove(key);
	}
	public void lock(ResourceLocation key) {
		lock(key.toString());
	}
}
