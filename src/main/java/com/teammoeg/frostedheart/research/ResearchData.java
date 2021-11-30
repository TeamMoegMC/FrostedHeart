package com.teammoeg.frostedheart.research;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;

import java.util.HashSet;

public class ResearchData {
	public ResearchData(){}
	public ResearchData(CompoundNBT cn) {
		deserialize(cn);
	}
	public CompoundNBT serialize() {
		return new CompoundNBT();
		
	}
	public void deserialize(CompoundNBT cn) {
		
	}

	public boolean isCompleted() {
		return getProgress() == 1.0F;
	}

	//TODO: impl
	public boolean isInProgress() {
		return false;
	}

	//TODO: impl
	public HashSet<ItemStack> getItemStored() {
		HashSet<ItemStack> set = new HashSet<ItemStack>();
		set.add(new ItemStack(Items.GRASS_BLOCK));
		return set;
	}

	//TODO: impl
	public float getProgress() {
		return 0.0F;
	}
}
