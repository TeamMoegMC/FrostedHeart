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

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;

public class FertilizedFarmlandBlock extends FarmBlock {

	public FertilizedFarmlandBlock(Properties pProperties) {
		super(pProperties);
		this.registerDefaultState(this.getStateDefinition().any()
				.setValue(FertilizedDirt.FERTILIZER, Fertilizer.FertilizerType.ACCELERATED)
				.setValue(FertilizedDirt.STORAGE, 1).setValue(MOISTURE, 0)
				.setValue(FertilizedDirt.GRADE, Fertilizer.FertilizerGrade.BASIC));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder);
		pBuilder.add(FertilizedDirt.FERTILIZER, FertilizedDirt.GRADE, FertilizedDirt.STORAGE);
	}

	public void fallOn(Level pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float pFallDistance) {
		if (!pLevel.isClientSide && net.minecraftforge.common.ForgeHooks.onFarmlandTrample(pLevel, pPos,
				Blocks.DIRT.defaultBlockState(), pFallDistance, pEntity)) { // Forge: Move logic to Entity#canTrample
			turnToDirt(pEntity, pState, pLevel, pPos);
		}

		pEntity.causeFallDamage(pFallDistance, 1.0F, pEntity.damageSources().fall());
	}

	/**
	 * Performs a random tick on a block.
	 */
	public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
		int i = pState.getValue(MOISTURE);
		if (!isNearWater(pLevel, pPos) && !pLevel.isRainingAt(pPos.above())) {
			if (i > 0) {
				pLevel.setBlock(pPos, pState.setValue(MOISTURE, Integer.valueOf(i - 1)), 2);
			} else if (!shouldMaintainFarmland(pLevel, pPos)) {
				turnToDirt((Entity) null, pState, pLevel, pPos);
			}
		} else if (i < 7) {
			pLevel.setBlock(pPos, pState.setValue(MOISTURE, Integer.valueOf(7)), 2);
		}

	}

	private static boolean shouldMaintainFarmland(BlockGetter pLevel, BlockPos pPos) {
		BlockState plant = pLevel.getBlockState(pPos.above());
		BlockState state = pLevel.getBlockState(pPos);
		return plant.getBlock() instanceof net.minecraftforge.common.IPlantable && state.canSustainPlant(pLevel, pPos,
				Direction.UP, (net.minecraftforge.common.IPlantable) plant.getBlock());
	}

	private static boolean isNearWater(LevelReader pLevel, BlockPos pPos) {
		BlockState state = pLevel.getBlockState(pPos);
		for (BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-4, 0, -4), pPos.offset(4, 1, 4))) {
			if (state.canBeHydrated(pLevel, pPos, pLevel.getFluidState(blockpos), blockpos)) {
				return true;
			}
		}

		return net.minecraftforge.common.FarmlandWaterManager.hasBlockWaterTicket(pLevel, pPos);
	}

	public static void turnToDirt(@Nullable Entity pEntity, BlockState pState, Level pLevel, BlockPos pPos) {
		int storage = pState.getValue(FertilizedDirt.STORAGE);
		FertilizerType type = pState.getValue(FertilizedDirt.FERTILIZER);
		FertilizerGrade grade = pState.getValue(FertilizedDirt.GRADE);
		storage--;
		BlockState blockstate;
		if (storage <= 0) {
			blockstate = pushEntitiesUp(pState, Blocks.DIRT.defaultBlockState(), pLevel, pPos);
		} else {
			blockstate = pushEntitiesUp(pState,
					FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER, type)
							.setValue(FertilizedDirt.STORAGE, storage).setValue(FertilizedDirt.GRADE, grade),
					pLevel, pPos);
		}

		pLevel.setBlockAndUpdate(pPos, blockstate);
		pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
	}
}
