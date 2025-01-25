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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
/**
 * Cause bee stay inside hive when cold, prevents them from dying
 * <p>
 * */
@Mixin(BeehiveBlockEntity.class)
public class BeeHiveMixin_TempCheck extends BlockEntity {


    public BeeHiveMixin_TempCheck(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
	}

	@Inject(at = @At("HEAD"), method = "tickOccupants", cancellable = true)
	private static void tickOccupants(Level level, BlockPos worldPosition, BlockState pState, List pData, @Nullable BlockPos pSavedFlowerPos,CallbackInfo cbi) {
        if (!level.isClientSide && WorldTemperature.block(level, worldPosition) < 14)
            cbi.cancel();
    }
}
