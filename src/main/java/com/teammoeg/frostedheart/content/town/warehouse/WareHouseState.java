package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.nbt.CompoundTag;

public class WareHouseState extends WorkerState {
    int volume;//有效体积
    int area;//占地面积
    double capacity;//最大容量
	public WareHouseState() {
	}
	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		super.writeNBT(tag, isNetwork);
		tag.putDouble("capacity", this.capacity);
		tag.putInt("area", this.area);
		tag.putInt("volume", this.volume);
	}
	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		super.readNBT(tag, isNetwork);
		capacity=tag.getDouble("capacity");
		area=tag.getInt("area");
		volume=tag.getInt("volume");
	}

}
