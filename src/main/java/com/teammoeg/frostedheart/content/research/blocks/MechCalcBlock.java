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

package com.teammoeg.frostedheart.content.research.blocks;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHKineticBlock;
import com.teammoeg.frostedheart.util.TranslateUtils;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

public class MechCalcBlock extends FHKineticBlock {
    static final VoxelShaper shape = VoxelShaper.forDirectional(VoxelShapes.or(Block.makeCuboidShape(0, 0, 0, 16, 9, 16), Block.makeCuboidShape(0, 9, 0, 16, 16, 13)), Direction.SOUTH);

    public MechCalcBlock( Properties blockProps) {
        super(blockProps);
        this.setDefaultState(this.stateContainer.getBaseState().with(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
    }


    @Override
    public void addInformation(ItemStack stack, IBlockReader worldIn, List<ITextComponent> tooltip,
                               ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (stack.hasTag() && stack.getTag().getBoolean("prod")) {
            tooltip.add(TranslateUtils.str("For Display Only"));
        }
    }


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.MECH_CALC.get().create();
    }


    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        super.fillItemGroup(group, items);
        ItemStack is = new ItemStack(this);
        is.getOrCreateTag().putBoolean("prod", true);
        items.add(is);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.get(BlockStateProperties.HORIZONTAL_FACING).rotateY().getAxis();
    }


    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return shape.get(state.get(BlockStateProperties.HORIZONTAL_FACING));
    }


    @Override
    public boolean hasShaftTowards(IWorldReader arg0, BlockPos arg1, BlockState state, Direction dir) {
        return state.get(BlockStateProperties.HORIZONTAL_FACING).rotateY().getAxis() == dir.getAxis();
    }


    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }


    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
        if (superResult.isSuccessOrConsume() || player instanceof FakePlayer)
            return superResult;
        TileEntity te = Utils.getExistingTileEntity(world, pos);
        if (te instanceof MechCalcTileEntity)
            return ((MechCalcTileEntity) te).onClick(player);
        return superResult;
    }


    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getBoolean("prod")) {
            TileEntity te = Utils.getExistingTileEntity(worldIn, pos);
            if (te instanceof MechCalcTileEntity) {
                ((MechCalcTileEntity) te).doProduct = false;
            }
        }

        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

    }


	/*@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.of(16);
	}*/


}
