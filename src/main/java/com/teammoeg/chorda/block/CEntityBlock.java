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

package com.teammoeg.chorda.block;

import java.util.function.Supplier;

import com.teammoeg.chorda.block.entity.CTickableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
/**
 * Interface for entity block
 * */
public interface CEntityBlock<B extends BlockEntity> extends EntityBlock {
	@Override
	public default BlockEntity newBlockEntity(BlockPos p, BlockState s) {
		if(hasTileEntity(p,s))
			return getBlock().get().create(p, s);
		return null;
	}

	Supplier<BlockEntityType<B>> getBlock();
	public default boolean hasTileEntity(BlockPos p,BlockState state) {
		return true;
	}
	public default boolean hasTicker(BlockState state) {
		return true;
	}
	@Override
	public default <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState,
			BlockEntityType<T> pBlockEntityType) {
		if(hasTicker(pState))
		return new BlockEntityTicker<T>() {

			@Override
			public void tick(Level pLevel, BlockPos pPos, BlockState pState, BlockEntity pBlockEntity) {
				if (!pBlockEntity.hasLevel())
					pBlockEntity.setLevel(pLevel);
				if (pBlockEntity instanceof CTickableBlockEntity entity)
					entity.tick();
			}
		};
		return null;
	}



}
