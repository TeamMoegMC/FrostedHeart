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

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.NonMirrorableWithActiveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 活跃状态多方块结构方块，基于沉浸工程（Immersive Engineering）的不可镜像活跃方块扩展。
 * 默认初始化为非活跃状态，且具有较高的阴影亮度（0.8）以改善视觉效果。
 * <p>
 * Block for active multiblock structures, extending Immersive Engineering's
 * {@link NonMirrorableWithActiveBlock}. Initializes with the ACTIVE state set to false
 * by default and provides a higher shade brightness (0.8) for improved visual appearance.
 *
 * @param <S> 多方块状态类型 / the multiblock state type
 */
public class CActiveMultiblockBlock<S extends IMultiblockState> extends NonMirrorableWithActiveBlock<S> {

	/**
	 * 使用给定的方块属性和多方块注册信息构造活跃多方块方块，默认为非活跃状态。
	 * <p>
	 * Constructs an active multiblock block with the given properties and multiblock registration,
	 * defaulting to inactive state.
	 *
	 * @param properties 方块属性 / the block properties
	 * @param multiblock 多方块注册信息 / the multiblock registration
	 */
	public CActiveMultiblockBlock(Properties properties, MultiblockRegistration<S> multiblock) {
		super(properties, multiblock);
		super.registerDefaultState(super.defaultBlockState().setValue(NonMirrorableWithActiveBlock.ACTIVE, false));
	}

	/**
	 * 获取方块的阴影亮度。返回 0.8 以使多方块结构内部不会过暗。
	 * <p>
	 * Gets the shade brightness of this block. Returns 0.8 to prevent multiblock
	 * interiors from being too dark.
	 *
	 * @param pState 方块状态 / the block state
	 * @param pLevel 方块所在的世界访问器 / the block getter for world access
	 * @param pPos 方块位置 / the block position
	 * @return 阴影亮度值 / the shade brightness value
	 */
	@Override
	public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
		return 0.8f;
	}

}
