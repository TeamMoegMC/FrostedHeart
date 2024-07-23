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

import java.util.UUID;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.util.TranslateUtils;
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

import net.minecraft.block.AbstractBlock.Properties;

public class DrawingDeskBlock extends FHBaseBlock implements IModelOffsetProvider {

    public static final BooleanProperty IS_NOT_MAIN = BooleanProperty.create("not_multi_main");
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty BOOK = BooleanProperty.create("has_book");

    static final VoxelShape shape = Block.box(0, 0, 0, 16, 15, 16);
    static final VoxelShape shape2 = Block.box(0, 0, 0, 16, 12, 16);

    private static Direction getNeighbourDirection(boolean b, Direction directionIn) {
        return !b ? directionIn : directionIn.getOpposite();
    }

    public static void setBlockhasbook(World worldIn, BlockPos pos, BlockState state, boolean hasBook) {
        worldIn.setBlock(pos, state.setValue(BOOK, hasBook), 3);
    }

    public DrawingDeskBlock(Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_NOT_MAIN, false).setValue(BOOK, false));
        super.setLightOpacity(0);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        if (!state.getValue(IS_NOT_MAIN))
            return new DrawingDeskTileEntity();
        else return null;
    }

    @Override
    public boolean triggerEvent(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.triggerEvent(state, worldIn, pos, id, param);
        TileEntity tileentity = worldIn.getBlockEntity(pos);
        return tileentity != null && tileentity.triggerEvent(id, param);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(IS_NOT_MAIN, FACING, BOOK);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos,
                                        ISelectionContext context) {
        if (state.getValue(IS_NOT_MAIN))
            return shape2;
        return shape;
    }

    @Override
    public BlockPos getModelOffset(BlockState arg0, Vector3i arg1) {
        if (arg0.getValue(IS_NOT_MAIN))
            return new BlockPos(1, 0, 0);
        return new BlockPos(0, 0, 0);
        //return null;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (state.getValue(IS_NOT_MAIN))
            return shape2;
        return shape;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction direction = context.getHorizontalDirection().getClockWise();
        BlockPos blockpos = context.getClickedPos();
        BlockPos blockpos1 = blockpos.relative(direction);
        return context.getLevel().getBlockState(blockpos1).canBeReplaced(context) ? this.defaultBlockState().setValue(FACING, direction) : null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return !state.getValue(IS_NOT_MAIN);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isClientSide && handIn == Hand.MAIN_HAND && !player.isShiftKeyDown()) {
            if (!player.isCreative() && worldIn.getMaxLocalRawBrightness(pos) < 8) {
                player.displayClientMessage(TranslateUtils.translateMessage("research.too_dark"), true);
            } else if (!player.isCreative() && PlayerTemperatureData.getCapability(player).map(PlayerTemperatureData::getBodyTemp).orElse(0f) < -0.2) {
                player.displayClientMessage(TranslateUtils.translateMessage("research.too_cold"), true);
            } else {
                if (state.getValue(IS_NOT_MAIN)) {
                    pos = pos.relative(getNeighbourDirection(state.getValue(IS_NOT_MAIN), state.getValue(FACING)));
                }
                TileEntity ii = Utils.getExistingTileEntity(worldIn, pos);
                UUID crid = FHTeamDataManager.get(player).getId();
                IOwnerTile.trySetOwner(ii, crid);
                if (crid != null && crid.equals(IOwnerTile.getOwner(ii)))
                    NetworkHooks.openGui((ServerPlayerEntity) player, (IInteractionObjectIE) ii, ii.getBlockPos());
                else
                    player.displayClientMessage(TranslateUtils.translateMessage("research.not_owned"), true);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void playerWillDestroy(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!worldIn.isClientSide && player.isCreative()) {
            boolean block = state.getValue(IS_NOT_MAIN);
            if (!block) {
                BlockPos blockpos = pos.relative(getNeighbourDirection(state.getValue(IS_NOT_MAIN), state.getValue(FACING)));
                BlockState blockstate = worldIn.getBlockState(blockpos);
                if (blockstate.getBlock() == this && blockstate.getValue(IS_NOT_MAIN)) {
                    worldIn.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
        super.playerWillDestroy(worldIn, pos, state, player);
    }

    @Override
    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!worldIn.isClientSide) {
            BlockPos blockpos = pos.relative(state.getValue(FACING));
            worldIn.setBlock(blockpos, state.setValue(IS_NOT_MAIN, true), 3);
            worldIn.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(worldIn, pos, 3);
        }
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (facing == getNeighbourDirection(stateIn.getValue(IS_NOT_MAIN), stateIn.getValue(FACING)) && facingState.getBlock() != this) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
}

