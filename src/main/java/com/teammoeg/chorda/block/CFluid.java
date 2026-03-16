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

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/**
 * 简化的流体实现，始终作为源方块存在且不会流动。
 * 该流体没有桶物品，不会生成对应的方块，适用于纯虚拟的流体类型。
 * <p>
 * A simplified fluid implementation that always acts as a source block and does not flow.
 * This fluid has no bucket item, does not create a corresponding block in the world,
 * and is suitable for purely virtual fluid types.
 */
public class CFluid extends ForgeFlowingFluid {

	/**
	 * 使用给定的流体属性构造流体。
	 * <p>
	 * Constructs a fluid with the given properties.
	 *
	 * @param properties 流体属性 / the fluid properties
	 */
	public CFluid(Properties properties) {
		super(properties);
	}

	/**
	 * 获取源流体。由于此流体不流动，返回自身。
	 * <p>
	 * Gets the source fluid. Returns itself since this fluid does not flow.
	 *
	 * @return 源流体（自身） / the source fluid (this instance)
	 */
	@Override
	public Fluid getSource() {
		return this;

	}

	/**
	 * 获取流动状态的流体。由于此流体不流动，返回自身。
	 * <p>
	 * Gets the flowing fluid. Returns itself since this fluid does not flow.
	 *
	 * @return 流动流体（自身） / the flowing fluid (this instance)
	 */
	@Override
	public Fluid getFlowing() {
		return this;
	}

	/**
	 * 获取此流体的桶物品。返回 AIR 表示没有对应的桶。
	 * <p>
	 * Gets the bucket item for this fluid. Returns AIR indicating no bucket exists.
	 *
	 * @return 桶物品（空气/无） / the bucket item (air/none)
	 */
	@Override
	public Item getBucket() {
		return Items.AIR;
	}

	/**
	 * 创建此流体在世界中对应的方块状态。返回空气方块状态，表示此流体不会在世界中显示。
	 * <p>
	 * Creates the legacy block state for this fluid in the world. Returns air block state,
	 * indicating this fluid does not render as a block.
	 *
	 * @param state 流体状态 / the fluid state
	 * @return 对应的方块状态（空气） / the corresponding block state (air)
	 */
	@Override
	protected BlockState createLegacyBlock(FluidState state) {
		return Blocks.AIR.defaultBlockState();
	}

	/**
	 * 判断给定流体是否与此流体相同（引用比较）。
	 * <p>
	 * Checks whether the given fluid is the same as this fluid (reference equality).
	 *
	 * @param fluidIn 要比较的流体 / the fluid to compare
	 * @return 如果相同则返回 true / true if the fluid is the same instance
	 */
	@Override
	public boolean isSame(Fluid fluidIn) {
		return fluidIn == this;
	}

	/**
	 * 判断此流体状态是否为源方块。始终返回 true。
	 * <p>
	 * Checks whether this fluid state is a source block. Always returns true.
	 *
	 * @param p_207193_1_ 流体状态 / the fluid state
	 * @return 始终为 true / always true
	 */
	@Override
	public boolean isSource(FluidState p_207193_1_) {
		return true;
	}

	/**
	 * 获取流体的量（以 mB 为单位）。始终返回 1000（一桶）。
	 * <p>
	 * Gets the amount of this fluid in millibuckets. Always returns 1000 (one bucket).
	 *
	 * @param pState 流体状态 / the fluid state
	 * @return 流体量 / the fluid amount in millibuckets
	 */
	@Override
	public int getAmount(FluidState pState) {
		return 1000;
	}

}
