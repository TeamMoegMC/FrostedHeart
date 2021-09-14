package com.teammoeg.frostedheart.steamenergy;

import net.minecraft.item.ItemStack;

public interface IChargable {
	float charge(ItemStack stack,float value);
}
