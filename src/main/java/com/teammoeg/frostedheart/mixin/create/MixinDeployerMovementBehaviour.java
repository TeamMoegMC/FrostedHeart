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

import com.simibubi.create.content.contraptions.components.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.teammoeg.frostedheart.util.mixin.ISpeedContraption;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeployerMovementBehaviour.class)
public abstract class MixinDeployerMovementBehaviour extends MovementBehaviour {
    @Inject(method = "tick(Lcom/simibubi/create/content/contraptions/components/structureMovement/MovementContext;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/nbt/CompoundNBT;putInt(Ljava/lang/String;I)V",
                    ordinal = 0
            ), cancellable = true)

    public void doTimer(MovementContext m, CallbackInfo cbi) {
        Contraption c = m.contraption;
        if (c instanceof ISpeedContraption) {
            int timer = m.data.getInt("Timer");
            timer += MathHelper.clamp(Math.abs(((ISpeedContraption) c).getSpeed()) * 10, 1, 2560);
            m.data.putInt("Timer", timer);
            cbi.cancel();
        }

    }

    @ModifyConstant(method = "tick(Lcom/simibubi/create/content/contraptions/components/structureMovement/MovementContext;)V", remap = false, constant = @Constant(intValue = 20, ordinal = 0))
    public int getTimerTick(int in) {
        return 10000;
    }
}
