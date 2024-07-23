/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.snow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import snownee.snow.ModUtil;
import snownee.snow.SnowCommonConfig;
import snownee.snow.block.ModSnowBlock;

import net.minecraft.block.AbstractBlock.Properties;

@Mixin(ModSnowBlock.class)
public abstract class ModSnowBlockMixin extends SnowBlock {
    private static final VoxelShape[] SHAPES = new VoxelShape[]{VoxelShapes.empty(), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};

    public ModSnowBlockMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author yuesha-yc
     * @reason change snow collision shape to always be empty
     */
    @Overwrite(remap = false)
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (!ModUtil.terraforged && SnowCommonConfig.thinnerBoundingBox) {
            int layers = state.getValue(LAYERS);
            if (layers == 8) {
                return VoxelShapes.block();
            } else if (layers > 5) {
                BlockState upState = worldIn.getBlockState(pos.above());
                return !upState.getBlock().isAir(upState, worldIn, pos) ? VoxelShapes.block() : SHAPES[layers - 5];
            } else {
                return VoxelShapes.empty();
            }
        } else {
            return super.getCollisionShape(state, worldIn, pos, context);
        }
    }

}
