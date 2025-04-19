/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.agriculture;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RubberDandelionBlock extends BushBlock implements BonemealableBlock {
    
    public static final IntegerProperty RUBBER_DANDELION_AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D)};

    public RubberDandelionBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(RUBBER_DANDELION_AGE, 0));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUBBER_DANDELION_AGE);
    }
    
    public boolean canGrow(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        int i = pState.getValue(RUBBER_DANDELION_AGE);
        return  i < 3 &&
                pLevel.getRawBrightness(pPos.above(), 0) >= 9 &&
                //No growth in wild.
                pLevel.getBlockState(pPos.below()).is(Blocks.FARMLAND) ;
    }
    
    public void grow(ServerLevel pLevel, BlockPos pPos, BlockState pState, RandomSource pRandom, int growChance) {
        int i = pState.getValue(RUBBER_DANDELION_AGE);
        if (this.canGrow(pLevel, pPos, pState) && ForgeHooks.onCropsGrowPre(pLevel, pPos, pState, pRandom.nextInt(growChance) == 0)) {
            BlockState blockstate = pState.setValue(RUBBER_DANDELION_AGE, i + 1);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(blockstate));
            ForgeHooks.onCropsGrowPost(pLevel, pPos, pState);
        }
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        this.grow(pLevel, pPos, pState, pRandom, 5);
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[state.getValue(RUBBER_DANDELION_AGE)];
    }
    
    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
        return this.canGrow(pLevel, pPos, pState);
    }
    
    @Override
    public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        return true;
    }
    
    @Override
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        this.grow(pLevel, pPos, pState, pRandom, 1);
    }
}

