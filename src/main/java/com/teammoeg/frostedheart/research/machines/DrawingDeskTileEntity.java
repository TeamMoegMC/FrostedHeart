package com.teammoeg.frostedheart.research.machines;

import com.teammoeg.frostedheart.FHContent;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;

public class DrawingDeskTileEntity extends TileEntity implements IInteractionObjectIE,IIEInventory{
	protected NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    public DrawingDeskTileEntity() {
        super(FHContent.FHTileTypes.DRAWING_DESK.get());
    }
	@Override
	public boolean canUseGui(PlayerEntity arg0) {
		return true;
	}
	@Override
	public IInteractionObjectIE getGuiMaster() {
		return this;
	}
	@Override
	public void doGraphicalUpdates() {
	}
	@Override
	public NonNullList<ItemStack> getInventory() {
		return inventory;
	}
	@Override
	public int getSlotLimit(int arg0) {
		return 3;
	}
	@Override
	public boolean isStackValid(int arg0, ItemStack arg1) {
		return true;
	}

}
