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

import com.teammoeg.chorda.block.CBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class WarehouseStorageRackBlock extends CBlock {
    private static BooleanProperty VALID = BooleanProperty.create("valid");

    public WarehouseStorageRackBlock(Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(VALID, false));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VALID);
    }
    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        boolean valid = isRackValid(worldIn, pos);
        BlockState newState = this.stateDefinition.any().setValue(VALID, valid);
        worldIn.setBlockAndUpdate(pos, newState);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (fromPos.getY() != pos.getY()) return;
        boolean shouldBeValid = isRackValid(world, pos);
        if (state.getValue(VALID) != shouldBeValid) {
            world.setBlock(pos, state.setValue(VALID, shouldBeValid), 3);
        }
    }

    private static boolean isRackValid(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos.east()).isAir()
                && world.getBlockState(pos.west()).isAir()
                && world.getBlockState(pos.north()).isAir()
                && world.getBlockState(pos.south()).isAir();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 2;
    }

}


