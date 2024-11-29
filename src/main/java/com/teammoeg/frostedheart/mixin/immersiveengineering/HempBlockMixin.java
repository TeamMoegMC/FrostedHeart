/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.WorldTemperature.TemperatureCheckResult;
import com.teammoeg.frostedheart.util.FHUtils;

import blusunrize.immersiveengineering.common.blocks.plant.HempBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

@Mixin(HempBlock.class)
public class HempBlockMixin {


    @Inject(at=@At("HEAD"),method="randomTick",cancellable=true)
    private void fh$randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand,CallbackInfo cbi) {
        TemperatureCheckResult temp = WorldTemperature.isSuitableForCrop(world, pos, WorldTemperature.HEMP_GROW_TEMPERATURE, WorldTemperature.HEMP_GROW_TEMPERATURE-5);
        if(temp.isRipedOff()) {
        	FHUtils.setToAirPreserveFluid(world, pos);
        }else if(temp.isDeadly()) {
        	if (world.getRandom().nextInt(3) == 0) {
                if (state != state.getBlock().defaultBlockState())
                    world.setBlock(pos, state.getBlock().defaultBlockState(), 2);
            }
        }
        if (!temp.isSuitable()) {
            cbi.cancel();
        }
    }

}
