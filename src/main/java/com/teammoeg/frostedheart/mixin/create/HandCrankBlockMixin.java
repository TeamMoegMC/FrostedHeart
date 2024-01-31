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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.components.crank.HandCrankBlock;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

@Mixin(HandCrankBlock.class)
public class HandCrankBlockMixin {
    /**
     * @author khjxiaogu
     * @reason Disable fake player from making energy
     */
    @Inject(at = @At("INVOKE"), method = "onBlockActivated", cancellable = true, remap = true)
    public void onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<ActionResultType> ci) {
        if (player instanceof FakePlayer) {
            worldIn.destroyBlock(pos, true);
            ci.setReturnValue(ActionResultType.FAIL);
        } else if (player.getFoodStats().getFoodLevel() < 4) {
            if (player.getEntityWorld().isRemote)
                player.sendStatusMessage(GuiUtils.translateMessage("crank.feel_hunger"), true);
            ci.setReturnValue(ActionResultType.FAIL);
        }
    }
}
