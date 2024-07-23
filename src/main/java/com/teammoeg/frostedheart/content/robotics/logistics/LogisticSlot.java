package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.ILogisticsStorage;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.TileEntityLogisticsStorage;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LogisticSlot {
	public int slot;
	public TileEntityLogisticsStorage storage;
	public LogisticSlot(ILogisticsStorage storage,BlockEntity te, int slot) {
		super();
		this.slot = slot;
		this.storage = new TileEntityLogisticsStorage(storage,te);
	}
	public LogisticSlot(TileEntityLogisticsStorage storage, int slot) {
		super();
		this.storage = storage;
		this.slot = slot;
	}
	public <T extends BlockEntity&ILogisticsStorage> LogisticSlot(T storage, int slot) {
		this.slot = slot;
		this.storage = new TileEntityLogisticsStorage(storage);
	}
	public ItemStack getItem() {
		return storage.getInventory().getStackInSlot(slot);
	}
	public ItemStack insert(ItemStack stack) {
		return storage.getInventory().insertItem(slot, stack, false);
	}
	public ItemStack extract(int amt) {
		return storage.getInventory().extractItem(slot, amt, false);
	}
	public void setItem(ItemStack item) {
		storage.getInventory().setStackInSlot(slot, item);
	}
	public boolean exists() {
		return !storage.isRemoved();
	}
	public boolean hasSize(ItemStack stack) {
		return exists()&&Math.min(stack.getMaxDamage()-getItem().getCount(),storage.getInventory().getSlotLimit(slot)-getItem().getCount())>0;
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
		return Objects.equals(storage.getActualPos(), other.storage.getActualPos()) && slot == other.slot;
	}
}
