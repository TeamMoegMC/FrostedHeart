/*
 * Copyright (c) 2022-2024 TeamMoeg
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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;

@Mixin(DeployerMovementBehaviour.class)
public abstract class MixinDeployerMovementBehaviour implements MovementBehaviour {
    // TODO for some reason fails to inject when loaded in mc environment outside
//    @Inject(method = "tick(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;)V",
//            at = @At(value = "INVOKE",
//                    target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V",
//                    ordinal = 0
//            ), cancellable = true,remap=false)
//
//    public void doTimer(MovementContext m, CallbackInfo cbi) {
//        Contraption c = m.contraption;
//        if (c instanceof ISpeedContraption) {
//            int timer = m.data.getInt("Timer");
//            timer += (int) Mth.clamp(Math.abs(((ISpeedContraption) c).getSpeed()) * 10, 1, 2560);
//            m.data.putInt("Timer", timer);
//            cbi.cancel();
//        }
//
//    }
//
    @ModifyConstant(method = "tick(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;)V", remap = false, constant = @Constant(intValue = 20, ordinal = 0))
    public int getTimerTick(int in) {
        return 10000;
    }
}
