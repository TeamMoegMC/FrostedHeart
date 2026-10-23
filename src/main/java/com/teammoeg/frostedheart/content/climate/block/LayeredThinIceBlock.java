/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class LayeredThinIceBlock extends ThinIceBlock {
    public static final int MAX_HEIGHT = 8;
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    protected static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[]{Shapes.empty(), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    public static final int HEIGHT_IMPASSABLE = 5;

    public LayeredThinIceBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, Integer.valueOf(1)).setValue(CRACK, 0));
    }

    @Override
    public int fractureStages(BlockState state) {
        return (state.getValue(LAYERS) + 1) / 2;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // At 1/8 block height vanilla stepOn may address the block below the ice.
        double surface = pos.getY() + state.getValue(LAYERS) / 8.0;
        if (entity.onGround() && Math.abs(entity.getY() - surface) < 0.05)
            stepOn(level, pos, state, entity);
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        super.randomTick(pState, pLevel, pPos, pRandom);
    }

    /**
     * We skip rendering our face if the adjacent thin ice is taller than us.
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adj, Direction side) {
        if (adj.is(this) && adj.hasProperty(LAYERS) && state.hasProperty(LAYERS)) {
            if (adj.getValue(LAYERS) >= state.getValue(LAYERS)) {
                return true;
            }
        }
        // always taller
        if (adj.is(FHBlocks.THIN_ICE_BLOCK.get())) {
            return true;
        }
        return super.skipRendering(state, adj, side);
    }

    public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
        switch (pType) {
            case LAND:
                return pState.getValue(LAYERS) < HEIGHT_IMPASSABLE;
            case WATER:
                return false;
            case AIR:
                return false;
            default:
                return false;
        }
    }

    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_LAYER[pState.getValue(LAYERS)];
    }

    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_LAYER[pState.getValue(LAYERS)];
    }

    public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return SHAPE_BY_LAYER[pState.getValue(LAYERS)];
    }

    public VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_LAYER[pState.getValue(LAYERS)];
    }

    public boolean useShapeForLightOcclusion(BlockState pState) {
        return true;
    }

    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.getValue(LAYERS) == MAX_HEIGHT ? 0.2F : 1.0F;
    }

    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        int i = pState.getValue(LAYERS);
        if (pUseContext.getItemInHand().is(this.asItem()) && i < MAX_HEIGHT) {
            if (pUseContext.replacingClickedOnBlock()) {
                return pUseContext.getClickedFace() == Direction.UP;
            } else {
                return true;
            }
        } else {
            return i == 1;
        }
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos());
        if (blockstate.is(this)) {
            int i = blockstate.getValue(LAYERS);
            int nextLayers = Math.min(MAX_HEIGHT, i + 1);
            return blockstate.setValue(LAYERS, nextLayers).setValue(CRACK,
                    inheritedCrack(blockstate.getValue(CRACK), i, nextLayers));
        } else {
            return super.getStateForPlacement(pContext);
        }
    }

    public static int inheritedCrack(int crack, int oldLayers, int newLayers) {
        int oldStages = (oldLayers + 1) / 2, newStages = (newLayers + 1) / 2;
        return Math.min(newStages - 1, (crack * newStages + oldStages - 1) / oldStages);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(LAYERS);
    }

    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return true;
    }

}
