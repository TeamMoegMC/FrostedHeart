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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.teammoeg.frostedheart.compat.create.ContraptionCostUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin({LinearActuatorBlockEntity.class})
public abstract class MixinPulleyTileEntity extends KineticBlockEntity {

    public MixinPulleyTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}

	@Shadow(remap = false)
    public AbstractContraptionEntity movedContraption;

    private int fh$cooldown;


    @Override
    public float calculateStressApplied() {

        if (movedContraption != null && movedContraption.isAlive()) {
            fh$cooldown = 100;
            ContraptionCostUtils.setSpeedAndCollect(movedContraption, (int) speed);
            if (getMotionVector().y() < 0) {
                this.lastStressApplied = ContraptionCostUtils.getActorCost(movedContraption) + 0.5F;
                return lastStressApplied;
            }
            this.lastStressApplied = ContraptionCostUtils.getCost(movedContraption) + 1;
            return lastStressApplied;
        } else if (fh$cooldown <= 0) {
            this.lastStressApplied = 1;
            return lastStressApplied;
        } else fh$cooldown--;
        return lastStressApplied;
    }

    @Inject(at = @At("TAIL"), method = "tick",remap=false)
    public void FH_MICR_tick(CallbackInfo cbi) {
        if ((!level.isClientSide) && super.hasNetwork())
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }

    @Shadow(remap = false)
    public abstract Vec3 getMotionVector();
}
