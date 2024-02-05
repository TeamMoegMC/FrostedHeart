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

package com.teammoeg.frostedheart.research.machines;

import java.util.UUID;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.player.Temperature;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import blusunrize.immersiveengineering.api.client.IModelOffsetProvider;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class DrawingDeskBlock extends FHBaseBlock implements IModelOffsetProvider {

    public static final BooleanProperty IS_NOT_MAIN = BooleanProperty.create("not_multi_main");
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty BOOK = BooleanProperty.create("has_book");

    static final VoxelShape shape = Block.makeCuboidShape(0, 0, 0, 16, 15, 16);
    static final VoxelShape shape2 = Block.makeCuboidShape(0, 0, 0, 16, 12, 16);

    private static Direction getNeighbourDirection(boolean b, Direction directionIn) {
        return b == false ? directionIn : directionIn.getOpposite();
    }

    public static void setBlockhasbook(World worldIn, BlockPos pos, BlockState state, boolean hasBook) {
        worldIn.setBlockState(pos, state.with(BOOK, hasBook), 3);
    }

    public DrawingDeskBlock(Properties blockProps) {
        super(blockProps);
        this.setDefaultState(this.stateContainer.getBaseState().with(IS_NOT_MAIN, false).with(BOOK, false));
        super.setLightOpacity(0);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        if (!state.get(IS_NOT_MAIN))
            return new DrawingDeskTileEntity();
        else return null;
    }

    @Override
    public boolean eventReceived(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.eventReceived(state, worldIn, pos, id, param);
        TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity != null && tileentity.receiveClientEvent(id, param);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(IS_NOT_MAIN, FACING, BOOK);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos,
                                        ISelectionContext context) {
        if (state.get(IS_NOT_MAIN))
            return shape2;
        return shape;
    }

    @Override
    public BlockPos getModelOffset(BlockState arg0, Vector3i arg1) {
        if (arg0.get(IS_NOT_MAIN))
            return new BlockPos(1, 0, 0);
        return new BlockPos(0, 0, 0);
        //return null;
    }

    @Override
    public PushReaction getPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (state.get(IS_NOT_MAIN))
            return shape2;
        return shape;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction direction = context.getPlacementHorizontalFacing().rotateY();
        BlockPos blockpos = context.getPos();
        BlockPos blockpos1 = blockpos.offset(direction);
        return context.getWorld().getBlockState(blockpos1).isReplaceable(context) ? this.getDefaultState().with(FACING, direction) : null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return !state.get(IS_NOT_MAIN);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND && !player.isSneaking()) {
            if (!player.isCreative() && worldIn.getLight(pos) < 8) {
                player.sendStatusMessage(GuiUtils.translateMessage("research.too_dark"), true);
            } else if (!player.isCreative() && Temperature.getBodySmoothed(player) < -0.2) {
                player.sendStatusMessage(GuiUtils.translateMessage("research.too_cold"), true);
            } else {
                if (state.get(IS_NOT_MAIN)) {
                    pos = pos.offset(getNeighbourDirection(state.get(IS_NOT_MAIN), state.get(FACING)));
                }
                TileEntity ii = Utils.getExistingTileEntity(worldIn, pos);
                UUID crid = ResearchDataAPI.getData(player).getId();
                IOwnerTile.trySetOwner(ii, crid);
                if (crid != null && crid.equals(IOwnerTile.getOwner(ii)))
                    NetworkHooks.openGui((ServerPlayerEntity) player, (IInteractionObjectIE) ii, ii.getPos());
                else
                    player.sendStatusMessage(GuiUtils.translateMessage("research.not_owned"), true);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!worldIn.isRemote && player.isCreative()) {
            boolean block = state.get(IS_NOT_MAIN);
            if (block == false) {
                BlockPos blockpos = pos.offset(getNeighbourDirection(state.get(IS_NOT_MAIN), state.get(FACING)));
                BlockState blockstate = worldIn.getBlockState(blockpos);
                if (blockstate.getBlock() == this && blockstate.get(IS_NOT_MAIN) == true) {
                    worldIn.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 35);
                }
            }
        }
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!worldIn.isRemote) {
            BlockPos blockpos = pos.offset(state.get(FACING));
            worldIn.setBlockState(blockpos, state.with(IS_NOT_MAIN, true), 3);
            worldIn.updateBlock(pos, Blocks.AIR);
            state.updateNeighbours(worldIn, pos, 3);
        }
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state.with(FACING, direction.rotate(state.get(FACING)));
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (facing == getNeighbourDirection(stateIn.get(IS_NOT_MAIN), stateIn.get(FACING)) && facingState.getBlock() != this) {
            return Blocks.AIR.getDefaultState();
        }
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
}

