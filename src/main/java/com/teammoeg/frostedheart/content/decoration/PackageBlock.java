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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class PackageBlock extends CBlock {
    private static Integer typeCount = 4;
    private static IntegerProperty TYPE = IntegerProperty.create("packagetype", 0, typeCount - 1);
    private static Integer colorCount = 3;
    private static IntegerProperty COLOR = IntegerProperty.create("packagecolor", 0, colorCount - 1);
    static final VoxelShape shape = Block.box(0, 0, 0, 16, 15, 16);
    public PackageBlock(BlockBehaviour.Properties blockProps) {
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
    public void playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(worldIn, pos, state, player);
//        int count = Math.abs(worldIn.random.nextInt()) % 5 + 1;
//        popResource(worldIn, pos, new ItemStack(Items.POTATO, count));
    }

    @Override
    public void wasExploded(Level worldIn, BlockPos pos, Explosion explosionIn) {
        super.wasExploded(worldIn, pos, explosionIn);
//        int count = Math.abs(worldIn.random.nextInt()) % 2 + 1;
//        popResource(worldIn, pos, new ItemStack(Items.POTATO, count));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos,
                                        CollisionContext context) {
        return shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
