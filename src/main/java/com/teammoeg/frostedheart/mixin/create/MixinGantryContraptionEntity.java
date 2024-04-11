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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraptionEntity;
import com.simibubi.create.content.contraptions.relays.advanced.GantryShaftTileEntity;
import com.teammoeg.frostedheart.util.mixin.IGantryShaft;

import net.minecraft.entity.EntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

@Mixin(GantryContraptionEntity.class)
public abstract class MixinGantryContraptionEntity extends AbstractContraptionEntity {

    public MixinGantryContraptionEntity(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Inject(at = @At("HEAD"), method = "checkPinionShaft", remap = false)
    protected void checkPinionShaft(CallbackInfo cbi) {
        Direction facing = ((GantryContraption) contraption).getFacing();
        Vector3d currentPosition = getAnchorVec().add(.5, .5, .5);
        BlockPos gantryShaftPos = new BlockPos(currentPosition).offset(facing.getOpposite());

        TileEntity te = world.getTileEntity(gantryShaftPos);
        if (te instanceof IGantryShaft) {
            GantryShaftTileEntity gte = (GantryShaftTileEntity) te;
            ((IGantryShaft) gte).setEntity(this);
            gte.networkDirty = true;
        }
    }

}
