package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.AbstractPlantBlock;
import net.minecraft.block.AbstractTopPlantBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.server.ServerWorld;

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
	 */
	@Overwrite
	public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
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

	}
}
