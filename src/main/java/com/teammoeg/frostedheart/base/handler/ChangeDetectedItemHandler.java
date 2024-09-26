package com.teammoeg.frostedheart.base.handler;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

public class ChangeDetectedItemHandler implements IItemHandler{
	IItemHandler handler;
	BlockEntity block;
	public ChangeDetectedItemHandler(BlockEntity block,IItemHandler handler) {
		super();
		this.handler = handler;
		this.block = block;
	}
	public int getSlots() {
		return handler.getSlots();
	}
	public @NotNull ItemStack getStackInSlot(int slot) {
		return handler.getStackInSlot(slot);
	}
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		ItemStack ret= handler.insertItem(slot, stack, simulate);
		if(!simulate&&ret.getCount()!=stack.getCount())
			onContentsChanged(slot);
		return ret;
	}
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack extracted= handler.extractItem(slot, amount, simulate);
		if(!simulate&&!extracted.isEmpty())
			onContentsChanged(slot);
		return extracted;
	}
	public int getSlotLimit(int slot) {
		return handler.getSlotLimit(slot);
	}
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return handler.isItemValid(slot, stack);
	}
	protected void onContentsChanged(int slot) {
		block.setChanged();
	}

}
