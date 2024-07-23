/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.incubator;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHGuiBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.RegistryObject;

import net.minecraft.block.AbstractBlock.Properties;

public class IncubatorBlock extends FHGuiBlock {
    static DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    static BooleanProperty LIT = BlockStateProperties.LIT;
    private RegistryObject<TileEntityType<?>> type;

    public IncubatorBlock(Properties p, RegistryObject<TileEntityType<?>> type) {
        super(p);
        this.type = type;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return type.get().create();
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection()).setValue(LIT, false);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                             Hand hand, BlockRayTraceResult hit) {

        if (FluidUtil.interactWithFluidHandler(player, hand, world, pos, hit.getDirection()))
            return ActionResultType.SUCCESS;
        return super.use(state, world, pos, player, hand, hit);
    }


}
