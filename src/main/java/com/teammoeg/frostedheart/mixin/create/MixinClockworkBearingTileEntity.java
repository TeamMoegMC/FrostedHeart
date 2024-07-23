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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkBearingTileEntity;
import com.teammoeg.frostedheart.util.mixin.ContraptionCostUtils;

import net.minecraft.world.level.block.entity.BlockEntityType;

@Mixin(ClockworkBearingTileEntity.class)
public abstract class MixinClockworkBearingTileEntity extends KineticTileEntity {

    @Shadow(remap = false)
    protected ControlledContraptionEntity hourHand;

    @Shadow(remap = false)
    protected ControlledContraptionEntity minuteHand;
    private int fh$cooldown;
    public MixinClockworkBearingTileEntity(BlockEntityType<?> typeIn) {
        super(typeIn);
    }

    @Override
    public float calculateStressApplied() {
        float stress = 1;
        if (hourHand != null && hourHand.isAlive()) {
            ContraptionCostUtils.setSpeedAndCollect(hourHand, speed / 4F);
            stress += ContraptionCostUtils.getRotationCost(hourHand);
        }
        if (minuteHand != null && minuteHand.isAlive()) {
            ContraptionCostUtils.setSpeedAndCollect(minuteHand, speed / 4F);
            stress += ContraptionCostUtils.getRotationCost(minuteHand);
        }
        if (stress == 1 && lastStressApplied > 1) {
            if (fh$cooldown <= 0) {
                this.lastStressApplied = stress;
            } else fh$cooldown--;
        } else {
            fh$cooldown = 100;
            this.lastStressApplied = stress;
        }
        return lastStressApplied;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void FH_MICR_tick(CallbackInfo cbi) {
        if ((!level.isClientSide) && super.hasNetwork())
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }
}
