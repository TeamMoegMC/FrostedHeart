package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class LogisticSlot {
	public BlockPos pos;
	public ILogisticsStorage storage;
	public int slot;
	
	public LogisticSlot(BlockPos pos, ILogisticsStorage storage, int slot) {
		super();
		this.pos = pos;
		this.storage = storage;
		this.slot = slot;
	}
	public ItemStack getItem() {
		return storage.getInventory().getStackInSlot(slot);
	}
	@Override
	public int hashCode() {
		return Objects.hash(pos, slot);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		LogisticSlot other = (LogisticSlot) obj;
		return Objects.equals(pos, other.pos) && slot == other.slot;
	}
}
