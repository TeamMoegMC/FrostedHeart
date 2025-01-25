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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.function.Supplier;

import com.teammoeg.chorda.block.CGuiBlock;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class LogisticChestBlock<T extends BlockEntity> extends CGuiBlock<T> {
	Supplier<BlockEntityType<T>> blockEntity;


	public LogisticChestBlock(Properties blockProps, Supplier<BlockEntityType<T>> blockEntity) {
		super(blockProps);
		this.blockEntity = blockEntity;
	}


	@Override
	public Supplier<BlockEntityType<T>> getBlock() {
		return blockEntity;
	}


}
