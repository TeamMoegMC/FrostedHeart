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

package com.teammoeg.frostedheart.content.steamenergy.debug;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

public class DebugHeaterBlock extends FHBaseBlock {
    public DebugHeaterBlock(Properties blockProps) {
        super(blockProps);
    }




    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.DEBUGHEATER.get().create();
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LEVEL_FLOWING);
    }


    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }


    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                             Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.use(state, world, pos, player, hand, hit);
        if (superResult.consumesAction() || player.isShiftKeyDown())
            return superResult;
        ItemStack item = player.getItemInHand(hand);
        if (item.getItem().equals(Item.byBlock(this))) {
            state = state.cycle(BlockStateProperties.LEVEL_FLOWING);
            world.setBlockAndUpdate(pos, state);
            player.displayClientMessage(TranslateUtils.str(String.valueOf(state.getValue(BlockStateProperties.LEVEL_FLOWING))), true);
            return ActionResultType.SUCCESS;
        }
        return superResult;
    }
}
