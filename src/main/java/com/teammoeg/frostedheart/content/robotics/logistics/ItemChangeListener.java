package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.world.item.ItemStack;

public interface ItemChangeListener {
	void onSlotChange(int slot,ItemStack after);
	void onSlotClear(int slot);
	void onCountChange(int slot,int before,int after);
}
