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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.gantry.GantryContraption;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.teammoeg.frostedheart.compat.create.ContraptionCostUtils;
import com.teammoeg.frostedheart.compat.create.IGantryShaft;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@Mixin(GantryShaftBlockEntity.class)
public abstract class MixinGantryShaftTileEntity extends KineticBlockEntity implements IGantryShaft {
    public MixinGantryShaftTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}

	private int fh$cooldown;

    public AbstractContraptionEntity currentComp;


    @Override
    public float calculateStressApplied() {
        if (currentComp != null) {
            if (currentComp.isAlive()) {
                fh$cooldown = 100;
                //float impact = currentComp.getContraption().getBlocks().size()*4;
                Direction facing = ((GantryContraption) currentComp.getContraption()).getFacing();
                Vec3 currentPosition = currentComp.getAnchorVec().add(.5, .5, .5);
                BlockPos gantryShaftPos = BlockPos.containing(currentPosition).relative(facing.getOpposite());
                if (gantryShaftPos.equals(this.worldPosition)) {
                    ContraptionCostUtils.setSpeedAndCollect(currentComp, (int) speed);
                    this.lastStressApplied = ContraptionCostUtils.getCost(currentComp) + 0.5F;
                    return lastStressApplied;
                }
                this.lastStressApplied = 0;
                return lastStressApplied;
            } else if (fh$cooldown <= 0) {
                currentComp = null;
                this.lastStressApplied = 0;
                return lastStressApplied;
            } else fh$cooldown--;
        }

        return lastStressApplied;
    }

    @Override
    public void setEntity(AbstractContraptionEntity comp) {
        currentComp = comp;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide && super.hasNetwork() && currentComp != null) {
            this.getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
    }
}
