package com.teammoeg.frostedheart.research;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Supplier;

public class ResearchData {
	boolean active;
	Supplier<Research> rs;
	int committed;//points committed
	final TeamResearchData parent;
	ArrayList<ItemStack> committedItems=new ArrayList<>();//items comitted
	public ResearchData(Supplier<Research> r,TeamResearchData parent){
		this.rs=r;
		this.parent=parent;
	}
	public int getCommitted() {
		return committed;
	}
	public int getTotalCommitted() {
		Research r=rs.get();
		int currentProgress=committed;
		for(AbstractClue ac:r.getClues())
			if(ac.isCompleted(parent))
				currentProgress+=r.getRequiredPoints()*ac.getResearchContribution();
		return currentProgress;
	}
	public ResearchData(Supplier<Research> r,CompoundNBT nc,TeamResearchData parent){
		this(r,parent);
		deserialize(nc);
	}
	public Research getResearch() {
		return rs.get();
	}
	public CompoundNBT serialize() {
		CompoundNBT cnbt=new CompoundNBT();
		//cnbt.putInt("research",getResearch().getRId());
		return cnbt;
		
	}
	public void deserialize(CompoundNBT cn) {
		//rs=FHResearch.getResearch(cn.getInt("research"));
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
		return getTotalCommitted()/rs.get().getRequiredPoints();
	}
}
