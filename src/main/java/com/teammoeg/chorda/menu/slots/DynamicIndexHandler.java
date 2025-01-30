package com.teammoeg.chorda.menu.slots;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DynamicIndexHandler extends Slot
{
    public DynamicIndexHandler(Container pContainer, int pSlot, int pX, int pY) {
		super(pContainer, pSlot, pX, pY);
	}

	private int index;


    @Override
    public boolean mayPlace(ItemStack stack)
    {
        if (stack.isEmpty())
            return false;
        return true;
    }

    @Override
    public ItemStack getItem()
    {
        return container.getItem(index);
    }

    @Override
    public void set(ItemStack stack)
    {
    	container.setItem(index, stack);
        this.setChanged();
    }


    @Override
    public ItemStack remove(int amount)
    {
    	return this.container.removeItem(index, amount);
    }

}
