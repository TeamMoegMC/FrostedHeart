/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.rankine;



import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cannolicatfish.rankine.blocks.RankineLeavesBlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome.RainType;
import net.minecraft.world.server.ServerWorld;
@Mixin(RankineLeavesBlock.class)
public class MixinRankineLeavesBlock extends LeavesBlock {
	public MixinRankineLeavesBlock(Properties properties) {
		super(properties);
	}
	@Inject(at=@At("TAIL"),method="<init>",remap=false)
	public void fh$init(Properties properties,CallbackInfo cbi) {
		this.setDefaultState(this.stateContainer.getBaseState().with(DISTANCE, 7).with(PERSISTENT, Boolean.FALSE).with(BlockStateProperties.AGE_0_5, 4));
	}
	/**
	 * @author khjxiaogu
	 * @reason Fix a laging bug.
	 * */
	@Overwrite
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        BlockState bs = worldIn.getBlockState(pos.down());
        if (worldIn.getBlockState(pos.up()).matchesBlock(Blocks.SNOW)) {
        	if(!state.get(BlockStateProperties.AGE_0_5).equals(5))
            worldIn.setBlockState(pos, state.with(BlockStateProperties.AGE_0_5, 5),2);
        } else if ((worldIn.getBiome(pos).getPrecipitation() == RainType.SNOW || worldIn.getBiome(pos).getTemperature(pos) < 0.15)) {
        	if(!state.get(BlockStateProperties.AGE_0_5).equals(4))
        		worldIn.setBlockState(pos, state.with(BlockStateProperties.AGE_0_5, 4),2);
        } else if (!state.get(BlockStateProperties.AGE_0_5).equals(0)) {
            worldIn.setBlockState(pos, state.with(BlockStateProperties.AGE_0_5, 0),2);
        }
        super.randomTick(state, worldIn, pos, random);
    }
}
