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

package com.teammoeg.chorda.multiblock.components;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;

/**
 * 基于 Codec 的多方块状态数据接口。
 * 实现此接口的类作为 {@link CCodecState} 的数据载体，需要提供多方块初始化时的状态设置逻辑。
 * <p>
 * Interface for Codec-based multiblock state data.
 * Classes implementing this interface serve as data carriers for {@link CCodecState} and must
 * provide state initialization logic during multiblock setup.
 *
 * @see CCodecState
 */
public interface CCodecStateData {

	/**
	 * 设置多方块的初始状态。在多方块首次成型时调用，用于根据多方块上下文初始化数据。
	 * <p>
	 * Sets the initial state of the multiblock. Called when the multiblock is first formed,
	 * used to initialize data based on the multiblock context.
	 *
	 * @param init 多方块初始化上下文 / The initial multiblock context
	 */
	void setInitialState(IInitialMultiblockContext<CCodecState<CCodecStateData>> init);
}
