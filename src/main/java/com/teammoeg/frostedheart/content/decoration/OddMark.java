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

package com.teammoeg.frostedheart.content.decoration;

import javax.annotation.Nullable;

import com.teammoeg.chorda.block.CBlock;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class OddMark extends CBlock {
    private static Integer typeCount = 5;
    private static IntegerProperty TYPE = IntegerProperty.create("marktype", 0, typeCount - 1);
    private static Integer colorCount = 2;
    private static IntegerProperty COLOR = IntegerProperty.create("markcolor", 0, colorCount - 1);
    static final VoxelShape shape = Block.box(0, 0, 0, 16, 1, 16);
    static final VoxelShape shape2 = Block.box(0, 0, 0, 16, 2, 16);
    public OddMark(BlockBehaviour.Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, 0).setValue(COLOR, 0));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
        builder.add(COLOR);
    }

    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Integer finalType = Math.abs(worldIn.random.nextInt()) % typeCount;
        Integer finalColor = Math.abs(worldIn.random.nextInt()) % colorCount;
        BlockState newState = this.stateDefinition.any().setValue(TYPE, finalType).setValue(COLOR, finalColor);
        worldIn.setBlockAndUpdate(pos, newState);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos,
                                        CollisionContext context) {
        if(state.getValue(TYPE) <= 1)return shape;
        return shape2;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if(state.getValue(TYPE) <= 1)return shape;
        return shape2;
    }
}
