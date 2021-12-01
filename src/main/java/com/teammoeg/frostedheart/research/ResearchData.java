package com.teammoeg.frostedheart.research;

import java.util.HashSet;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;

public class ResearchData {
	boolean active;
	Supplier<Research> rs;
	public ResearchData(Supplier<Research> r){
		this.rs=r;
	}
	public ResearchData(CompoundNBT nc){
		deserialize(nc);
	}
	public Research getResearch() {
		return rs.get();
	}
	public CompoundNBT serialize() {
		CompoundNBT cnbt=new CompoundNBT();
		cnbt.putInt("research",getResearch().getRId());
		return cnbt;
		
	}
	public void deserialize(CompoundNBT cn) {
		rs=FHResearch.getResearch(cn.getInt("research"));
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
