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

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * To fix an issue that forge event is not properly fired which causes our event mechanic breaks.
 * e.g. kelp breaking everything/growing even when cold
 * <p>
 * */
@Mixin(GrowingPlantHeadBlock.class)
public abstract class AbstractTopPlantBlockMixin extends GrowingPlantBlock {
    @Shadow
    private double growthChance;

    protected AbstractTopPlantBlockMixin(Properties properties, Direction growthDirection, VoxelShape shape,
                                         boolean breaksInWater) {
        super(properties, growthDirection, shape, breaksInWater);
    }

    @Shadow
    abstract boolean canGrowIn(BlockState state);

    /**
     *
     * @reason fix forge event bug
     * @author khjxiaogu
     */
    @Inject(at = @At("HEAD"), method = "randomTick", cancellable = true)

    public void fh$randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random,CallbackInfo cbi) {
        if (state.getValue(GrowingPlantHeadBlock.AGE) < 25 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn,
                pos, state,
                random.nextDouble() < this.growthChance)) {
            BlockPos blockpos = pos.relative(this.growthDirection);
            if (this.canGrowIn(worldIn.getBlockState(blockpos))) {
                worldIn.setBlockAndUpdate(blockpos, state.cycle(GrowingPlantHeadBlock.AGE));
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, blockpos,
                        worldIn.getBlockState(blockpos));
            }
        }
        cbi.cancel();

    }
}
