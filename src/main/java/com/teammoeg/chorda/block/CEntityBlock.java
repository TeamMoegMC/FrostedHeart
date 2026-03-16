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
 * 方块实体接口，为方块提供方块实体（BlockEntity）的创建和刻（tick）更新支持。
 * <p>
 * Interface for blocks that have associated block entities. Provides default implementations
 * for creating block entities and attaching tickers. Blocks implementing this interface
 * should supply a {@link BlockEntityType} via {@link #getBlock()}.
 *
 * @param <B> 方块实体类型 / the block entity type
 */
public interface CEntityBlock<B extends BlockEntity> extends EntityBlock {

	/**
	 * 在指定位置创建新的方块实体。如果 {@link #hasTileEntity(BlockPos, BlockState)} 返回 true，
	 * 则使用 {@link #getBlock()} 提供的类型创建实体。
	 * <p>
	 * Creates a new block entity at the given position. Delegates to the block entity type
	 * supplier if {@link #hasTileEntity(BlockPos, BlockState)} returns true.
	 *
	 * @param p 方块位置 / the block position
	 * @param s 方块状态 / the block state
	 * @return 新的方块实体，如果不需要则返回 null / a new block entity, or null if not needed
	 */
	@Override
	public default BlockEntity newBlockEntity(BlockPos p, BlockState s) {
		if(hasTileEntity(p,s))
			return getBlock().get().create(p, s);
		return null;
	}

	/**
	 * 获取此方块对应的方块实体类型的供应器。
	 * <p>
	 * Gets the supplier for the block entity type associated with this block.
	 *
	 * @return 方块实体类型供应器 / the block entity type supplier
	 */
	Supplier<BlockEntityType<B>> getBlock();

	/**
	 * 判断指定位置和状态下的方块是否拥有方块实体。默认返回 true。
	 * <p>
	 * Determines whether this block has a tile entity at the given position and state.
	 * Returns true by default.
	 *
	 * @param p 方块位置 / the block position
	 * @param state 方块状态 / the block state
	 * @return 如果有方块实体则返回 true / true if the block has a tile entity
	 */
	public default boolean hasTileEntity(BlockPos p,BlockState state) {
		return true;
	}

	/**
	 * 判断指定状态的方块是否需要刻更新器。默认返回 true。
	 * <p>
	 * Determines whether this block needs a ticker for the given state.
	 * Returns true by default.
	 *
	 * @param state 方块状态 / the block state
	 * @return 如果需要刻更新器则返回 true / true if the block needs a ticker
	 */
	public default boolean hasTicker(BlockState state) {
		return true;
	}

	/**
	 * 获取方块实体的刻更新器。如果 {@link #hasTicker(BlockState)} 返回 true，
	 * 则返回一个会调用 {@link CTickableBlockEntity#tick()} 的更新器。
	 * <p>
	 * Gets the block entity ticker. If {@link #hasTicker(BlockState)} returns true,
	 * returns a ticker that invokes {@link CTickableBlockEntity#tick()} on matching entities.
	 *
	 * @param pLevel 当前世界 / the level
	 * @param pState 方块状态 / the block state
	 * @param pBlockEntityType 方块实体类型 / the block entity type
	 * @param <T> 方块实体泛型类型 / the block entity generic type
	 * @return 刻更新器，如果不需要则返回 null / the ticker, or null if not needed
	 */
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
