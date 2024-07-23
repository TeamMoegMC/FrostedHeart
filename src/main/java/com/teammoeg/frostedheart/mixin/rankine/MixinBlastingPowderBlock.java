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

package com.teammoeg.frostedheart.mixin.rankine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.blocks.BlastingPowderBlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

@Mixin(BlastingPowderBlock.class)
public class MixinBlastingPowderBlock extends FallingBlock {

    public MixinBlastingPowderBlock(Properties properties) {
        super(properties);
    }

    /**
     * @author khjxiaogu
     * @reason Fix dupe
     */
    @Override
    @Overwrite(remap = false)
    public void catchFire(BlockState state, World world, BlockPos pos, net.minecraft.util.Direction face,
                          LivingEntity igniter) {
        world.removeBlock(pos, false);
        world.explode(igniter, pos.getX(), pos.getY() + 16 * .0625D, pos.getZ(), 2.4F, Explosion.Mode.BREAK);
    }

}
