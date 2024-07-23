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

import net.minecraft.block.AbstractBlock.Properties;

public class MechCalcBlock extends FHKineticBlock {
    static final VoxelShaper shape = VoxelShaper.forDirectional(VoxelShapes.or(Block.box(0, 0, 0, 16, 9, 16), Block.box(0, 9, 0, 16, 16, 13)), Direction.SOUTH);

    public MechCalcBlock( Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
    }


    @Override
    public void appendHoverText(ItemStack stack, IBlockReader worldIn, List<ITextComponent> tooltip,
                               ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
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
    public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
        super.fillItemCategory(group, items);
        ItemStack is = new ItemStack(this);
        is.getOrCreateTag().putBoolean("prod", true);
        items.add(is);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise().getAxis();
    }


    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return shape.get(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }


    @Override
    public boolean hasShaftTowards(IWorldReader arg0, BlockPos arg1, BlockState state, Direction dir) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise().getAxis() == dir.getAxis();
    }


    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }


    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.use(state, world, pos, player, hand, hit);
        if (superResult.consumesAction() || player instanceof FakePlayer)
            return superResult;
        TileEntity te = Utils.getExistingTileEntity(world, pos);
        if (te instanceof MechCalcTileEntity)
            return ((MechCalcTileEntity) te).onClick(player);
        return superResult;
    }


    @Override
    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getBoolean("prod")) {
            TileEntity te = Utils.getExistingTileEntity(worldIn, pos);
            if (te instanceof MechCalcTileEntity) {
                ((MechCalcTileEntity) te).doProduct = false;
            }
        }

        super.setPlacedBy(worldIn, pos, state, placer, stack);

    }


	/*@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.of(16);
	}*/


}
