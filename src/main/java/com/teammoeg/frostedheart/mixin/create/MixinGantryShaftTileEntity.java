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
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.relays.advanced.GantryShaftTileEntity;
import com.teammoeg.frostedheart.util.mixin.ContraptionCostUtils;
import com.teammoeg.frostedheart.util.mixin.IGantryShaft;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GantryShaftTileEntity.class)
public abstract class MixinGantryShaftTileEntity extends KineticTileEntity implements ITickableTileEntity, IGantryShaft {
    private int fh$cooldown;

    public AbstractContraptionEntity currentComp;

    public MixinGantryShaftTileEntity(TileEntityType<?> typeIn) {
        super(typeIn);
    }
    @Override
    public float calculateStressApplied() {
        if (currentComp != null) {
            if (currentComp.isAlive()) {
                fh$cooldown = 100;
                //float impact = currentComp.getContraption().getBlocks().size()*4;
                Direction facing = ((GantryContraption) currentComp.getContraption()).getFacing();
                Vector3d currentPosition = currentComp.getAnchorVec().add(.5, .5, .5);
                BlockPos gantryShaftPos = new BlockPos(currentPosition).offset(facing.getOpposite());
                if (gantryShaftPos.equals(this.pos)) {
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
        if (!world.isRemote && super.hasNetwork() && currentComp != null) {
            this.getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
    }
}
