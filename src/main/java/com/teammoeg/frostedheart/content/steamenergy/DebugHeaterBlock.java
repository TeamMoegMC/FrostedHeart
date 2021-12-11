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

package com.teammoeg.frostedheart.content.steamenergy;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class DebugHeaterBlock extends FHBaseBlock implements ISteamEnergyBlock {
    public DebugHeaterBlock(String name, Properties blockProps,
                            BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
    }


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHContent.FHTileTypes.DEBUGHEATER.get().create();
    }
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        TileEntity te = Utils.getExistingTileEntity(worldIn, fromPos);
        if (te instanceof IConnectable) {
            Vector3i vec = pos.subtract(fromPos);
            Direction dir = Direction.getFacingFromVector(vec.getX(), vec.getY(), vec.getZ());
            ((IConnectable) te).connectAt(dir);
        }
    }

    @Override
    protected void fillStateContainer(Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(BlockStateProperties.LEVEL_1_8);
    }

    @Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player,
			Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
        if (superResult.isSuccessOrConsume() || player.isSneaking())
            return superResult;
        ItemStack item = player.getHeldItem(hand);
        if (item.getItem().equals(Item.getItemFromBlock(this))) {
        	state=state.cycleValue(BlockStateProperties.LEVEL_1_8);
        	world.setBlockState(pos, state);
        	player.sendStatusMessage(new StringTextComponent(String.valueOf(state.get(BlockStateProperties.LEVEL_1_8))),true);
            return ActionResultType.SUCCESS;
        }
        return superResult;
	}


	@Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }


    @Override
    public boolean canConnectFrom(IWorld world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }
}
