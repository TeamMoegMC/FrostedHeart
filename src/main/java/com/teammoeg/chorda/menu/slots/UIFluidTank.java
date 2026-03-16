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

package com.teammoeg.chorda.menu.slots;

import org.jetbrains.annotations.NotNull;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * 用于UI显示的流体槽，实现 {@link IFluidTank} 接口，通过数据槽在客户端和服务端之间同步流体数据。
 * <p>
 * Fluid tank for UI display purposes, implementing {@link IFluidTank}, that synchronizes fluid data
 * between client and server via data slots. This is a read-only display tank -- fill and drain operations
 * are no-ops since it is only used for rendering fluid information in GUI menus.
 */
public class UIFluidTank implements IFluidTank{
	/** 流体数据同步槽 / Data slot for fluid synchronization */
	CDataSlot<FluidStack> FLUID;
	/** 流体槽容量（毫桶） / Tank capacity in millibuckets */
	int capacity;

	/**
	 * 构造一个UI流体槽并在菜单中注册数据槽。
	 * <p>
	 * Constructs a UI fluid tank and registers a data slot in the menu.
	 *
	 * @param menu 要注册数据槽的基础菜单 / The base menu to register data slots in
	 * @param capacity 流体槽容量（毫桶） / The tank capacity in millibuckets
	 */
	public UIFluidTank(CBaseMenu menu,int capacity) {
		FLUID=CCustomMenuSlot.SLOT_TANK.create(menu);
		this.capacity=capacity;
	}

	/**
	 * 将此UI流体槽绑定到实际的流体槽，以便同步流体数据。
	 * <p>
	 * Binds this UI fluid tank to an actual fluid tank for data synchronization.
	 *
	 * @param tank 要绑定的流体槽 / The fluid tank to bind to
	 */
	public void bind(FluidTank tank) {
		FLUID.bind(()->tank.getFluid(),t->tank.setFluid(t));
	}

	/**
	 * 获取当前同步的流体堆。
	 * <p>
	 * Gets the currently synchronized fluid stack.
	 *
	 * @return 当前流体堆 / The current fluid stack
	 */
	@Override
	public @NotNull FluidStack getFluid() {
		return FLUID.getValue();
	}

	/**
	 * 获取当前流体量（毫桶）。
	 * <p>
	 * Gets the current fluid amount in millibuckets.
	 *
	 * @return 当前流体量 / The current fluid amount
	 */
	@Override
	public int getFluidAmount() {
		return FLUID.getValue().getAmount();
	}

	/**
	 * 获取流体槽容量。
	 * <p>
	 * Gets the tank capacity.
	 *
	 * @return 流体槽容量（毫桶） / The tank capacity in millibuckets
	 */
	@Override
	public int getCapacity() {
		return capacity;
	}

	/**
	 * 检查流体是否对此槽有效。作为UI显示槽，接受所有流体。
	 * <p>
	 * Checks if a fluid is valid for this tank. As a UI display tank, all fluids are accepted.
	 *
	 * @param stack 要检查的流体堆 / The fluid stack to check
	 * @return 始终返回true / Always returns true
	 */
	@Override
	public boolean isFluidValid(FluidStack stack) {
		return true;
	}

	/**
	 * 填充操作（无操作）。此为仅用于显示的UI槽，不支持填充。
	 * <p>
	 * Fill operation (no-op). This is a display-only UI tank that does not support filling.
	 *
	 * @param resource 要填充的流体 / The fluid to fill
	 * @param action 流体操作类型（模拟或执行） / The fluid action type (simulate or execute)
	 * @return 始终返回0 / Always returns 0
	 */
	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return 0;
	}

	/**
	 * 按量排出操作（无操作）。此为仅用于显示的UI槽，不支持排出。
	 * <p>
	 * Drain by amount operation (no-op). This is a display-only UI tank that does not support draining.
	 *
	 * @param maxDrain 最大排出量 / The maximum amount to drain
	 * @param action 流体操作类型（模拟或执行） / The fluid action type (simulate or execute)
	 * @return 始终返回空流体堆 / Always returns an empty fluid stack
	 */
	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		return FluidStack.EMPTY;
	}

	/**
	 * 按流体类型排出操作（无操作）。此为仅用于显示的UI槽，不支持排出。
	 * <p>
	 * Drain by fluid type operation (no-op). This is a display-only UI tank that does not support draining.
	 *
	 * @param resource 要排出的流体类型 / The fluid type to drain
	 * @param action 流体操作类型（模拟或执行） / The fluid action type (simulate or execute)
	 * @return 始终返回空流体堆 / Always returns an empty fluid stack
	 */
	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}

}