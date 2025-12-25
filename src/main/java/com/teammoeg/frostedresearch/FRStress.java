package com.teammoeg.frostedresearch;

import com.simibubi.create.content.kinetics.BlockStressValues.IStressValueProvider;
import com.simibubi.create.foundation.utility.Couple;

import net.minecraft.world.level.block.Block;

public class FRStress implements IStressValueProvider {

    public FRStress() {
    }

    @Override
    public double getCapacity(Block arg0) {
        return 0;
    }

    @Override
    public double getImpact(Block arg0) {
        if (arg0 == FRContents.Blocks.MECHANICAL_CALCULATOR.get()) return 64;
        
        return 0;
    }

    @Override
    public boolean hasCapacity(Block arg0) {
        return false;
    }

    @Override
    public boolean hasImpact(Block arg0) {
        return arg0 == FRContents.Blocks.MECHANICAL_CALCULATOR.get();
    }

	@Override
	public Couple<Integer> getGeneratedRPM(Block block) {
		return null;
	}

}