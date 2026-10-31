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

package com.teammoeg.frostedheart.mixin.minecraft.thermal;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin_Melt {
    /**
     * @author TeamMoeg
     * @reason Material phase energy owns melting; vanilla light must not bypass it.
     */
    @Overwrite
    public void randomTick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {}

    // Vanilla has already handled drops and Silk Touch before this branch.
    @Inject(method = "playerDestroy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;dimensionType()Lnet/minecraft/world/level/dimension/DimensionType;"),
            cancellable = true)
    private void frostedheart$keepColdIce(Level level, Player player, BlockPos position,
            BlockState state, BlockEntity entity, ItemStack tool, CallbackInfo callback) {
        if (!(WorldTemperature.block(level, position) > WorldTemperature.WATER_ICE_MELTS)) callback.cancel();
    }
}
