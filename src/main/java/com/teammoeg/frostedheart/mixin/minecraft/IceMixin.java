package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

import net.minecraft.block.BlockState;
import net.minecraft.block.IceBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mixin(IceBlock.class)
public abstract class IceMixin {
	/**
	 * Performs a random tick on a block.
	 */
	@Overwrite
	public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
		if (worldIn.getLightFor(LightType.BLOCK, pos) > 11 - state.getOpacity(worldIn, pos)||ChunkData.getTemperature(worldIn, pos)>0) {
			this.turnIntoWater(state, worldIn, pos);
		}

	}

	@Shadow
	protected abstract void turnIntoWater(BlockState state, World world, BlockPos pos);
}
