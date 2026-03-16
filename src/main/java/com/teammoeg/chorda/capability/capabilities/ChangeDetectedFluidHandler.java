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

package com.teammoeg.chorda.capability.capabilities;

import com.teammoeg.chorda.block.entity.SyncableBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * 流体处理器的变更检测包装器。
 * 包装一个现有的{@link IFluidHandler}，在流体填充或排出时自动触发变更回调。
 * 通常用于方块实体中，当流体储罐发生变化时自动调用{@code setChanged()}或同步数据。
 * <p>
 * Change-detection wrapper for fluid handlers.
 * Wraps an existing {@link IFluidHandler} and automatically triggers a change callback
 * when fluids are filled or drained.
 * Typically used in block entities to automatically call {@code setChanged()} or sync data
 * when fluid tank contents change.
 */
public class ChangeDetectedFluidHandler implements IFluidHandler {
	/** 内容变更时的回调 / Callback invoked when contents change */
	Runnable onchange;
	/** 被包装的流体处理器 / The wrapped fluid handler */
	IFluidHandler nested;
	/**
	 * 使用指定的流体处理器和变更回调构造包装器。
	 * <p>
	 * Constructs a wrapper with the specified fluid handler and change callback.
	 *
	 * @param nested 被包装的流体处理器 / The fluid handler to wrap
	 * @param onchange 内容变更时的回调 / The callback to invoke when contents change
	 */
	public ChangeDetectedFluidHandler(IFluidHandler nested,Runnable onchange) {
		super();
		this.onchange = onchange;
		this.nested = nested;
	}

	/**
	 * 从方块实体创建一个包装器，变更时调用{@code setChanged()}。
	 * <p>
	 * Creates a wrapper from a block entity that calls {@code setChanged()} on changes.
	 *
	 * @param blockEntity 拥有此流体储罐的方块实体 / The block entity owning this fluid tank
	 * @param nested 被包装的流体处理器 / The fluid handler to wrap
	 * @param <T> 方块实体类型 / The block entity type
	 * @return 变更检测流体处理器 / A change-detected fluid handler
	 */
	public static <T extends BlockEntity> IFluidHandler fromBESetChanged(T blockEntity,IFluidHandler nested) {
		return new ChangeDetectedFluidHandler(nested,()->{blockEntity.setChanged();});
	}
	
	/**
	 * 从可同步的方块实体创建一个包装器，变更时同时调用{@code setChanged()}和{@code syncData()}。
	 * <p>
	 * Creates a wrapper from a syncable block entity that calls both {@code setChanged()} and
	 * {@code syncData()} on changes.
	 *
	 * @param blockEntity 拥有此流体储罐的可同步方块实体 / The syncable block entity owning this fluid tank
	 * @param nested 被包装的流体处理器 / The fluid handler to wrap
	 * @param <T> 方块实体类型，必须同时实现SyncableBlockEntity / The block entity type, must also implement SyncableBlockEntity
	 * @return 变更检测流体处理器 / A change-detected fluid handler
	 */
	public static <T extends BlockEntity& SyncableBlockEntity> IFluidHandler fromBESynced(T blockEntity, IFluidHandler nested) {
		return new ChangeDetectedFluidHandler(nested,()->{blockEntity.setChanged();blockEntity.syncData();});
	}
	/** {@inheritDoc} */
	@Override
	public int getTanks() {
		return nested.getTanks();
	}

	/** {@inheritDoc} */
	@Override
	public FluidStack getFluidInTank(int t) {
		return nested.getFluidInTank(t);
	}

	/** {@inheritDoc} */
	@Override
	public int getTankCapacity(int t) {
		return nested.getTankCapacity(t);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isFluidValid(int t, FluidStack stack) {
		return nested.isFluidValid(t, stack);
	}

	/**
	 * 向储罐填充流体。如果实际执行且成功填充了流体，则触发变更回调。
	 * <p>
	 * Fills the tank with fluid. Triggers the change callback if the action is executed
	 * and fluid was actually filled.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public int fill(FluidStack resource, FluidAction action) {
		int filled = nested.fill(resource, action);
		if (filled != 0 && action.execute()) {
			onContentsChanged();
		}
		return filled;
	}

	/**
	 * 按指定流体类型排出流体。如果实际执行且成功排出了流体，则触发变更回调。
	 * <p>
	 * Drains fluid matching the specified FluidStack. Triggers the change callback if the action
	 * is executed and fluid was actually drained.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		FluidStack drained = nested.drain(resource, action);
		if (!drained.isEmpty() && action.execute()) {
			onContentsChanged();
		}
		return drained;
	}

	/**
	 * 按指定最大量排出流体。如果实际执行且成功排出了流体，则触发变更回调。
	 * <p>
	 * Drains up to the specified maximum amount of fluid. Triggers the change callback if the action
	 * is executed and fluid was actually drained.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		FluidStack drained = nested.drain(maxDrain, action);
		if (!drained.isEmpty() && action.execute()) {
			onContentsChanged();
		}
		return drained;
	}
	/**
	 * 当流体内容发生变化时调用。执行构造时提供的变更回调。
	 * <p>
	 * Called when fluid contents change. Executes the change callback provided at construction.
	 */
	protected void onContentsChanged() {
		onchange.run();
	}

}
