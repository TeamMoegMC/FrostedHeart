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

package com.teammoeg.frostedheart.content.decoration;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;

import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

public class RelicChestBlock extends FHBaseBlock {
    protected static final VoxelShape X_SHAPE = Block.box(0D, 0D, 2D, 16D, 13D, 14D);
    protected static final VoxelShape Z_SHAPE = Block.box(2D, 0D, 0D, 14D, 13D, 16D);
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public RelicChestBlock() {
        super(Block.Properties.of(Material.METAL).sound(SoundType.STONE).requiresCorrectToolForDrops()
                .harvestTool(ToolType.PICKAXE).strength(35, 600).noOcclusion());
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return FHTileTypes.RELIC_CHEST.get().create();
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (state.getValue(HORIZONTAL_AXIS) == Direction.Axis.X)
            return X_SHAPE;
        return Z_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isClientSide) {
            RelicChestTileEntity tile = (RelicChestTileEntity) worldIn.getBlockEntity(pos);
            if (tile != null) {
                NetworkHooks.openGui((ServerPlayerEntity) player, tile, tile.getBlockPos());
                worldIn.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3F, 1.5F);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            TileEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof IIEInventory) {
                InventoryHelper.dropContents(worldIn, pos, (IInventory) tileentity);
            }

            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        switch (rot) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch (state.getValue(HORIZONTAL_AXIS)) {
                    case Z:
                        return state.setValue(HORIZONTAL_AXIS, Direction.Axis.X);
                    case X:
                        return state.setValue(HORIZONTAL_AXIS, Direction.Axis.Z);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }
}
