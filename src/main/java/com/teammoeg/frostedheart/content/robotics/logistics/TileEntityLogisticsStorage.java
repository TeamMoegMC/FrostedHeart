package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityLogisticsStorage implements ILogisticsStorage {
	public ILogisticsStorage storage;
	public TileEntity te;
	public TileEntityLogisticsStorage(ILogisticsStorage storage,TileEntity te) {
		this.storage = storage;
		this.te=te;
	}
	public <T extends TileEntity&ILogisticsStorage> TileEntityLogisticsStorage(T storage) {
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
		return te.getPos();
	}

	public World getActualWorld() {
		return te.getWorld();
	}

	public boolean isRemoved() {
		return te.isRemoved();
	}

}
