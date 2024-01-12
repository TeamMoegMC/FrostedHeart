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

package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.block.AbstractPlantBlock;
import net.minecraft.block.AbstractTopPlantBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(AbstractTopPlantBlock.class)
public abstract class AbstractTopPlantBlockMixin extends AbstractPlantBlock {
	@Shadow
	private double growthChance;

	protected AbstractTopPlantBlockMixin(Properties properties, Direction growthDirection, VoxelShape shape,
			boolean breaksInWater) {
		super(properties, growthDirection, shape, breaksInWater);
	}

	@Shadow
	abstract boolean canGrowIn(BlockState state);

	/**
	 * Performs a random tick on a block.
	 * @reason fix forge event bug
	 * @author khjxiaogu
	 */
	@Inject(at=@At("HEAD"),method="randomTick",cancellable=true,remap=true)
	
	public void fh$randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random,CallbackInfo cbi) {
		if (state.get(AbstractTopPlantBlock.AGE) < 25 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn,
				pos.offset(this.growthDirection), state,
				random.nextDouble() < this.growthChance)) {
			BlockPos blockpos = pos.offset(this.growthDirection);
			if (this.canGrowIn(worldIn.getBlockState(blockpos))) {
				worldIn.setBlockState(blockpos, state.cycleValue(AbstractTopPlantBlock.AGE));
				net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, blockpos,
						worldIn.getBlockState(blockpos));
			}
		}
		cbi.cancel();

	}
}
