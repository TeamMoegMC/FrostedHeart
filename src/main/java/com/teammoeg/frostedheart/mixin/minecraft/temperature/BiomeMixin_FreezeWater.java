/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
/**
 * Freeze water when cold
 * <p>
 * */
@Mixin(Biome.class)
public abstract class BiomeMixin_FreezeWater {
//	/**
//	 * @author khjxiaogu
//	 * @reason decide when to freeze water
//	 * */
//	@Overwrite
//    public boolean shouldFreeze(LevelReader pLevel, BlockPos pWater, boolean pMustBeAtEdge) {
//	      if (this.warmEnoughToRain(pWater)) {
//	          return false;
//	       } else {
//	          if (pWater.getY() >= pLevel.getMinBuildHeight()
//					  && pWater.getY() < pLevel.getMaxBuildHeight()
//					  && pLevel.getBrightness(LightLayer.BLOCK, pWater) < 10
//					  &&  WorldTemperature.block(pLevel, pWater) < WorldTemperature.WATER_FREEZES) {
//	             BlockState blockstate = pLevel.getBlockState(pWater);
//	             FluidState fluidstate = pLevel.getFluidState(pWater);
//	             if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
//	                if (!pMustBeAtEdge) {
//	                   return true;
//	                }
//
//	                boolean flag = pLevel.isWaterAt(pWater.west()) && pLevel.isWaterAt(pWater.east()) && pLevel.isWaterAt(pWater.north()) && pLevel.isWaterAt(pWater.south());
//	                if (!flag) {
//	                   return true;
//	                }
//	             }
//	          }
//
//	          return false;
//	       }
//    }

	/**
	 * @author yuesha
	 * @reason Disable vanilla freezing logic. We handle it on our own.
	 * */
	@Overwrite
	public boolean shouldFreeze(LevelReader pLevel, BlockPos pWater, boolean pMustBeAtEdge) {
		return false;
	}

    @Shadow
    public abstract boolean warmEnoughToRain(BlockPos pPos);
}
