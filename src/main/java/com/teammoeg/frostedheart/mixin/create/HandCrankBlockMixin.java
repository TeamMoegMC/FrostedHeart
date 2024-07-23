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
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;

@Mixin(HandCrankBlock.class)
public class HandCrankBlockMixin {
    /**
     * @author khjxiaogu
     * @reason Disable fake player from making energy
     */
    @Inject(at = @At("INVOKE"), method = "onBlockActivated", cancellable = true)
    public void onBlockActivated(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        if (player instanceof FakePlayer) {
            worldIn.destroyBlock(pos, true);
            ci.setReturnValue(InteractionResult.FAIL);
        } else if (player.getFoodData().getFoodLevel() < 4) {
            if (player.getCommandSenderWorld().isClientSide)
                player.displayClientMessage(TranslateUtils.translateMessage("crank.feel_hunger"), true);
            ci.setReturnValue(InteractionResult.FAIL);
        }
    }
}
