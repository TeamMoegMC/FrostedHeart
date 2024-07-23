package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import java.util.Objects;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityLogisticsStorage implements ILogisticsStorage {
	public ILogisticsStorage storage;
	public BlockEntity te;
	public TileEntityLogisticsStorage(ILogisticsStorage storage,BlockEntity te) {
		this.storage = storage;
		this.te=te;
	}
	public <T extends BlockEntity&ILogisticsStorage> TileEntityLogisticsStorage(T storage) {
		this.storage = storage;
		te=storage;
	}
	@Override
	public ItemStackHandler getInventory() {
		return storage.getInventory();
	}

	@Override
	public boolean isValidFor(ItemStack stack) {
		return storage.isValidFor(stack);
	}

	public BlockPos getActualPos() {
		return te.getBlockPos();
	}

	public Level getActualWorld() {
		return te.getLevel();
	}

	public boolean isRemoved() {
		return te.isRemoved();
	}
	public ILogisticsStorage getStorage() {
		return storage;
	}
	public BlockEntity getTe() {
		return te;
	}
	@Override
	public int hashCode() {
		return Objects.hash(te);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TileEntityLogisticsStorage other = (TileEntityLogisticsStorage) obj;
		return this.te==other.te;
	}

}
