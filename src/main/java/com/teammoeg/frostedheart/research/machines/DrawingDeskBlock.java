/*
 *  Copyright (c) 2021. TeamMoeg
 *
 *  This file is part of Energy Level Transition.
 *
 *  Energy Level Transition is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 *
 *  Energy Level Transition is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Energy Level Transition.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.research.machines;

import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.ResearchLevel;
import com.teammoeg.frostedheart.research.gui.ResearchScreen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
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
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class DrawingDeskBlock extends FHBaseBlock {

    public static final BooleanProperty IS_NOT_MAIN = BooleanProperty.create("not_multi_main");
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty BOOK = BooleanProperty.create("has_book");

    public DrawingDeskBlock(String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
        this.setDefaultState(this.stateContainer.getBaseState().with(IS_NOT_MAIN, false).with(BOOK, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(IS_NOT_MAIN, FACING, BOOK);
    }

    private static Direction getNeighbourDirection(boolean b, Direction directionIn) {
        return b == false ? directionIn : directionIn.getOpposite();
    }

    public static void setBlockhasbook(World worldIn, BlockPos pos, BlockState state, boolean hasBook) {
        worldIn.setBlockState(pos, state.with(BOOK, hasBook), 3);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state.with(FACING, direction.rotate(state.get(FACING)));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction direction = context.getPlacementHorizontalFacing().rotateYCCW();
        BlockPos blockpos = context.getPos();
        BlockPos blockpos1 = blockpos.offset(direction);
        return context.getWorld().getBlockState(blockpos1).isReplaceable(context) ? this.getDefaultState().with(FACING, direction) : null;
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!worldIn.isRemote) {
            boolean block = state.get(IS_NOT_MAIN);
            if (block == false) {
                BlockPos blockpos = pos.offset(getNeighbourDirection(state.get(IS_NOT_MAIN), state.get(FACING)));
                BlockState blockstate = worldIn.getBlockState(blockpos);
                if (blockstate.getBlock() == this && blockstate.get(IS_NOT_MAIN) == true) {
                    worldIn.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 35);
                    worldIn.playEvent(player, 2001, blockpos, Block.getStateId(blockstate));
                }
            }
        }
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (facing == getNeighbourDirection(stateIn.get(IS_NOT_MAIN), stateIn.get(FACING)) && facingState.getBlock() != this) {
            return Blocks.AIR.getDefaultState();
        } else {
            return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
        }
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
    public PushReaction getPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        if (!state.get(IS_NOT_MAIN))
            return new DrawingDeskTileEntity();
        else return null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return !state.get(IS_NOT_MAIN);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND) {
            if (state.get(IS_NOT_MAIN)) {
                pos = pos.offset(getNeighbourDirection(state.get(IS_NOT_MAIN), state.get(FACING)));
            }
        }
        //todo: actually add some server-side functions in TE to provide the level and in progress research
        ResearchScreen screen = new ResearchScreen(player, ResearchLevel.DRAWING_DESK, FHResearch.researches.getByName("generator_t1"));
        screen.openGui();
        return ActionResultType.SUCCESS;
    }

    @Override
    public boolean eventReceived(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.eventReceived(state, worldIn, pos, id, param);
        TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity != null && tileentity.receiveClientEvent(id, param);
    }
}

