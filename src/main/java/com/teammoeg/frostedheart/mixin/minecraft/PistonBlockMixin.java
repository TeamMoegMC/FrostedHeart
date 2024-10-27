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

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockMixin extends DirectionalBlock {
    protected PistonBlockMixin(Properties builder) {
        super(builder);
    }

    @ModifyConstant(method = "moveBlocks", constant = @Constant(intValue = 68, ordinal = 0))
    public int getFlag(int in) {
        return in | 16;
    }
}
