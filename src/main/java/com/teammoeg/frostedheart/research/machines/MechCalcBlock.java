/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.research.machines;

import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHKineticBlock;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class MechCalcBlock extends FHKineticBlock{
	static final VoxelShaper shape=VoxelShaper.forDirectional(VoxelShapes.or(Block.makeCuboidShape(0, 0, 0,16, 9,16),Block.makeCuboidShape(0, 9, 0,16,16,13)),Direction.SOUTH);
    public MechCalcBlock(String name, Properties blockProps,
                        BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
        this.setDefaultState(this.stateContainer.getBaseState().with(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
    }


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.MECH_CALC.get().create();
    }


    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
        if (superResult.isSuccessOrConsume() || player.isSneaking())
            return superResult;
        TileEntity te=Utils.getExistingTileEntity(world, pos);
        if(te instanceof MechCalcTileEntity)
        	return ((MechCalcTileEntity) te).onClick(player);
        return superResult;
    }


    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return shape.get(state.get(BlockStateProperties.HORIZONTAL_FACING));
	}


	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(BlockStateProperties.HORIZONTAL_FACING).rotateY().getAxis();
	}


	@Override
	public boolean hasShaftTowards(IWorldReader arg0, BlockPos arg1, BlockState state, Direction dir) {
		return state.get(BlockStateProperties.HORIZONTAL_FACING).rotateY().getAxis()==dir.getAxis();
	}


	/*@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.of(16);
	}*/



	
}
