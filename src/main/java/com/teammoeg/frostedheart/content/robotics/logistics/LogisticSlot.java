package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class LogisticSlot {
	public ILogisticsStorage storage;
	public int slot;
	
	public LogisticSlot(ILogisticsStorage storage, int slot) {
		super();
		this.storage = storage;
		this.slot = slot;
	}
	public ItemStack getItem() {
		return storage.getInventory().getStackInSlot(slot);
	}
	public void setItem(ItemStack item) {
		storage.getInventory().setStackInSlot(slot, item);
	}
	public boolean hasSize(ItemStack stack) {
		return Math.min(stack.getMaxDamage()-getItem().getCount(),storage.getInventory().getSlotLimit(slot)-getItem().getCount())>0;
	}
	@Override
	public int hashCode() {
		return Objects.hash(storage.getActualPos(), slot);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		LogisticSlot other = (LogisticSlot) obj;
		return Objects.equals(storage, other.storage) && slot == other.slot;
	}
}
