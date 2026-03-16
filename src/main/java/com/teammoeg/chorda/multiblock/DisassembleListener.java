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

package com.teammoeg.chorda.multiblock;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;

/**
 * 多方块拆解事件的监听器接口。
 * 当一个 {@link CMultiblock} 被拆解时，如果其逻辑组件实现了此接口，
 * 则会在实际拆解之前收到通知，允许执行自定义的清理或保存逻辑。
 * <p>
 * Listener interface for multiblock disassembly events.
 * When a {@link CMultiblock} is disassembled, if its logic component implements this interface,
 * it will be notified before the actual disassembly, allowing custom cleanup or save logic to be executed.
 *
 * @param <S> 多方块状态类型 / The multiblock state type
 * @see CMultiblock#disassemble(net.minecraft.world.level.Level, BlockPos, boolean, net.minecraft.core.Direction)
 */
public interface DisassembleListener<S extends IMultiblockState> {

	/**
	 * 当多方块即将被拆解时调用。此方法在实际拆解逻辑执行之前被调用，
	 * 可用于保存数据、释放资源或执行其他清理操作。
	 * <p>
	 * Called when the multiblock is about to be disassembled. This method is invoked before
	 * the actual disassembly logic executes, and can be used to save data, release resources,
	 * or perform other cleanup operations.
	 *
	 * @param block 正在被拆解的多方块定义 / The multiblock definition being disassembled
	 * @param helper 多方块方块实体辅助器，提供对状态的访问 / The multiblock block entity helper providing access to the state
	 */
	void onDisassemble(IMultiblock block,IMultiblockBEHelper<S> helper);
}
