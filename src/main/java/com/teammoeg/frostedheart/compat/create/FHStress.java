/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.compat.create;

import com.simibubi.create.content.kinetics.BlockStressValues.IStressValueProvider;
import com.simibubi.create.foundation.utility.Couple;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.world.level.block.Block;

public class FHStress implements IStressValueProvider {

    public FHStress() {
    }

    @Override
    public double getCapacity(Block arg0) {
    	if(arg0 ==FHBlocks.STEAM_CORE.get())return FHConfig.COMMON.steamCoreCapacity.get();
        return 0;
    }

    @Override
    public double getImpact(Block arg0) {
        if (arg0 == FHBlocks.MECHANICAL_CALCULATOR.get()) return 64;
        
        return 0;
    }

    @Override
    public boolean hasCapacity(Block arg0) {
        return arg0 == FHBlocks.STEAM_CORE.get();
    }

    @Override
    public boolean hasImpact(Block arg0) {
        return arg0 == FHBlocks.MECHANICAL_CALCULATOR.get();
    }

	@Override
	public Couple<Integer> getGeneratedRPM(Block block) {
		if(block==FHBlocks.STEAM_CORE.get()) {
			int rpm=(int)(double)FHConfig.COMMON.steamCoreGeneratedSpeed.get();
			return Couple.create(rpm,rpm);
		}
		return null;
	}

}
