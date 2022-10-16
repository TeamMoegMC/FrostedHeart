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

package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.components.actors.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.contraptions.components.actors.SawMovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.utility.TreeCutter;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SawMovementBehaviour.class)
public abstract class MixinSawMovementBehaviour extends BlockBreakingMovementBehaviour {
    @Inject(at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/TreeCutter;findTree(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lcom/simibubi/create/foundation/utility/TreeCutter$Tree;",
            ordinal = 0, remap = false),
            method = "onBlockBroken", cancellable = true, remap = false)
    private void FH$onBroken(MovementContext context, BlockPos pos, BlockState brokenState, CallbackInfo cbi) {
        if (context.world == null)
            return;
        BlockState up = context.world.getBlockState(pos.up());
        if (TreeCutter.isVerticalPlant(brokenState) && !TreeCutter.isVerticalPlant(up))
            cbi.cancel();
        if (TreeCutter.isChorus(brokenState) && !TreeCutter.isChorus(up))
            cbi.cancel();
    }
}
