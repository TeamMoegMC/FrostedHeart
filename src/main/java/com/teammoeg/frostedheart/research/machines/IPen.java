package com.teammoeg.frostedheart.research.machines;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface IPen {
	
	boolean canUse(PlayerEntity e, ItemStack stack,int val);
	void doDamage(PlayerEntity e, ItemStack stack,int val);
	default boolean damage(PlayerEntity e, ItemStack stack,int val) {
		if(canUse(e,stack,val)) {
			doDamage(e,stack,val);
			return true;
		}
		return false;
	}
}
