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

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.LinearActuatorTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LinearActuatorTileEntity.class})
public abstract class MixinPulleyTileEntity extends KineticTileEntity {

    @Shadow(remap = false)
    public AbstractContraptionEntity movedContraption;

    private int fh$cooldown;

    public MixinPulleyTileEntity(TileEntityType<?> typeIn) {
        super(typeIn);
    }

    @Override
    public float calculateStressApplied() {

        if (movedContraption != null && movedContraption.isAlive()) {
            fh$cooldown = 100;
            ContraptionCostUtils.setSpeedAndCollect(movedContraption, (int) speed);
            if (getMotionVector().getY() < 0) {
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

    @Inject(at = @At("TAIL"), method = "tick")
    public void FH_MICR_tick(CallbackInfo cbi) {
        if ((!world.isRemote) && super.hasNetwork())
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }

    @Shadow(remap = false)
    public abstract Vector3d getMotionVector();
}
