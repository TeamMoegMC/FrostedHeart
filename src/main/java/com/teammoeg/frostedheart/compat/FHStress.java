package com.teammoeg.frostedheart.compat;

import com.simibubi.create.foundation.block.BlockStressValues.IStressValueProvider;
import com.teammoeg.frostedheart.FHBlocks;

import net.minecraft.block.Block;

public class FHStress implements IStressValueProvider {

	public FHStress() {
	}

	@Override
	public double getCapacity(Block arg0) {
		
		return 0;
	}

	@Override
	public double getImpact(Block arg0) {
		if(arg0==FHBlocks.mech_calc)return 64;
		return 0;
	}

	@Override
	public boolean hasCapacity(Block arg0) {
		return false;
	}

	@Override
	public boolean hasImpact(Block arg0) {
		if(arg0==FHBlocks.mech_calc)return true;
		return false;
	}

}
