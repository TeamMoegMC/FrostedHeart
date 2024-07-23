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

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import blusunrize.immersiveengineering.common.util.RotationUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(RotationUtil.class)
public class RotationUtilMixin {
    private final static ResourceLocation tag = new ResourceLocation("immersiveengineering", "no_rotation");

    /**
     * @author khjxiaogu
     * @reason fix some rotation bug
     */
    @Overwrite(remap = false)
    public static boolean rotateBlock(Level world, BlockPos pos, boolean inverse) {
        if (!world.getBlockState(pos).getBlock().getTags().contains(tag))
            return RotationUtil.rotateBlock(world, pos, inverse ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90);
        return false;
    }

}
