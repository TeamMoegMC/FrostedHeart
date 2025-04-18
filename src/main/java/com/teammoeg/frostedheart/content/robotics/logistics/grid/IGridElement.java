package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import java.util.Map;

import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import net.minecraft.world.item.ItemStack;

public interface IGridElement {

	int getEmptySlotCount();

	default ItemStack pushItem(ItemStack is,boolean fillEmpty) {
		return pushItem(new ItemKey(is),is,fillEmpty);
	}

	ItemStack pushItem(ItemKey ik, ItemStack is,boolean fillEmpty);

	ItemStack takeItem(ItemKey key, int amount);

	default ItemStack takeItem(ItemStack is) {
		return takeItem(new ItemKey(is),is.getCount());
	}
	Map<ItemKey, ? extends ItemCountProvider> getAllItems();
	
	boolean isChanged();
	void tick();
	boolean consumeChange();

}