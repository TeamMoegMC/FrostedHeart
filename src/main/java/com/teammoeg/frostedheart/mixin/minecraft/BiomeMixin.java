package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;

@Mixin(Biome.class)
public abstract class BiomeMixin {
	@Shadow
	public abstract float getTemperature(BlockPos pos);

	public boolean doesWaterFreeze(IWorldReader worldIn, BlockPos water, boolean mustBeAtEdge) {
		if (this.getTemperature(water) >= 0.15F) {
			return false;
		}
		if (water.getY() >= 0 && water.getY() < 256 && worldIn.getLightFor(LightType.BLOCK, water) < 10&&ChunkData.getTemperature(worldIn,water)<0) {
			BlockState blockstate = worldIn.getBlockState(water);
			FluidState fluidstate = worldIn.getFluidState(water);
			if (fluidstate.getFluid() == Fluids.WATER && blockstate.getBlock() instanceof FlowingFluidBlock) {
				if (!mustBeAtEdge) {
					return true;
				}

				boolean flag = worldIn.hasWater(water.west()) && worldIn.hasWater(water.east())
						&& worldIn.hasWater(water.north()) && worldIn.hasWater(water.south());
				if (!flag) {
					return true;
				}
			}
		}

		return false;
	}
}
