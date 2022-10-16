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

import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;
import net.minecraft.tileentity.TileEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MechanicalBearingTileEntity.class)
public abstract class MixinMechanicalBearingTileEntity extends GeneratingKineticTileEntity
        implements IBearingTileEntity {
    public MixinMechanicalBearingTileEntity(TileEntityType<?> typeIn) {
        super(typeIn);
    }
    
    @Shadow(remap = false)
    protected ControlledContraptionEntity movedContraption;
    private int fh$cooldown;
    @Override
    public float calculateStressApplied() {
        if (movedContraption != null&&movedContraption.isAlive()) {
        	fh$cooldown=100;
            ContraptionCostUtils.setSpeedAndCollect(movedContraption, (int) speed);
            this.lastStressApplied = ContraptionCostUtils.getRotationCost(movedContraption) + 1;
        } else if(fh$cooldown<=0) {
            this.lastStressApplied = 0.5F;
        }else fh$cooldown--;
        return lastStressApplied;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void FH_MICR_tick(CallbackInfo cbi) {
        if ((!world.isRemote) && super.hasNetwork())
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }
}
