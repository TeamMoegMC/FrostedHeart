package com.teammoeg.frostedheart.research;

import net.minecraft.nbt.CompoundNBT;

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
}
