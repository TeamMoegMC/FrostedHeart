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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CauldronBlock.class)
public class CauldronBlockMixin {

    /**
     * @reason Check temperature before lava drips.
     */
    @Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
    private void onReceiveStalactiteDrip(BlockState pState, Level pLevel, BlockPos pPos, Fluid pFluid, CallbackInfo ci) {
        // Only intercept lava processing
        if (pFluid == Fluids.LAVA) {
            // Add your custom check here
            if (WorldTemperature.block(pLevel, pPos) < WorldTemperature.LAVA_FREEZES) {
                // Cancel the original method execution if check fails
                ci.cancel();
            }
        }
        // For water or other fluids, proceed with original method
    }
}
