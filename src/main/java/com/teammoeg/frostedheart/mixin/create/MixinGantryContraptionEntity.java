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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.teammoeg.frostedheart.compat.create.IGantryShaft;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

@Mixin(GantryContraptionEntity.class)
public abstract class MixinGantryContraptionEntity extends AbstractContraptionEntity {

    public MixinGantryContraptionEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Inject(at = @At("HEAD"), method = "checkPinionShaft", remap = false)
    protected void checkPinionShaft(CallbackInfo cbi) {
        Direction facing = ((GantryContraption) contraption).getFacing();
        Vec3 currentPosition = getAnchorVec().add(.5, .5, .5);

        BlockPos gantryShaftPos = BlockPos.containing(currentPosition).relative(facing.getOpposite());

        BlockEntity te = level().getBlockEntity(gantryShaftPos);
        if (te instanceof IGantryShaft) {
            GantryShaftBlockEntity gte = (GantryShaftBlockEntity) te;
            ((IGantryShaft) gte).setEntity(this);
            gte.networkDirty = true;
        }
    }

}
