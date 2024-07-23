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

package com.teammoeg.frostedheart.content.steamenergy.charger;

import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

public class ChargerBlock extends FHBaseBlock{
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ChargerBlock(Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE).setValue(BlockStateProperties.FACING, Direction.SOUTH));
    }


    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(LIT)) {
            ClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.CHARGER.get().create();
    }


    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.FACING);
        builder.add(LIT);
    }


    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        if (context.getClickedFace() == Direction.UP || context.getPlayer().isShiftKeyDown()) {
            return this.defaultBlockState().setValue(BlockStateProperties.FACING, context.getNearestLookingDirection());
        }
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, context.getClickedFace().getOpposite());
    }


    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }


    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.use(state, world, pos, player, hand, hit);
        if (superResult.consumesAction() || player.isShiftKeyDown())
            return superResult;
        ItemStack item = player.getItemInHand(hand);

        TileEntity te = Utils.getExistingTileEntity(world, pos);
        if (te instanceof ChargerTileEntity) {
            return ((ChargerTileEntity) te).onClick(player, item);
        }
        return superResult;
    }


}
