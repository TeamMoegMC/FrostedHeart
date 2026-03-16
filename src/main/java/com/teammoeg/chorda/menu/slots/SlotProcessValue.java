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

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;

/**
 * 槽位处理进度值，用于在客户端和服务端之间同步加工/处理进度数据。
 * <p>
 * Slot process value used to synchronize processing/crafting progress data between client and server.
 * Manages two synchronized integer data slots: the current process value and the maximum process value,
 * which are typically used to render progress bars in GUI menus.
 */
public class SlotProcessValue {
	/** 当前加工进度数据槽 / Data slot for current processing progress */
	CDataSlot<Integer> PROCESS;
	/** 最大加工进度数据槽 / Data slot for maximum processing progress */
	CDataSlot<Integer> PROCESS_MAX;

	/**
	 * 构造一个处理进度值并在菜单中注册数据槽。
	 * <p>
	 * Constructs a process value and registers data slots in the menu.
	 *
	 * @param menu 要注册数据槽的基础菜单 / The base menu to register data slots in
	 */
	public SlotProcessValue(CBaseMenu menu) {
		PROCESS=CCustomMenuSlot.SLOT_INT.create(menu);
		PROCESS_MAX=CCustomMenuSlot.SLOT_INT.create(menu);
	}

	/**
	 * 获取当前加工进度值。
	 * <p>
	 * Gets the current processing progress value.
	 *
	 * @return 当前加工进度 / The current processing progress
	 */
	public int getProcess() {
		return PROCESS.getValue();
	}

	/**
	 * 获取最大加工进度值。
	 * <p>
	 * Gets the maximum processing progress value.
	 *
	 * @return 最大加工进度 / The maximum processing progress
	 */
	public int getProcessMax() {
		return PROCESS_MAX.getValue();
	}

	/**
	 * 绑定当前加工进度的获取器和设置器（用于服务端）。
	 * <p>
	 * Binds the getter and setter for current processing progress (used on server side).
	 *
	 * @param getter 进度值获取器 / The progress value getter
	 * @param setter 进度值设置器 / The progress value setter
	 */
	public void setProcess(Supplier<Integer> getter,Consumer<Integer> setter) {
		PROCESS.bind(getter, setter);
	}

	/**
	 * 绑定最大加工进度的获取器和设置器（用于服务端）。
	 * <p>
	 * Binds the getter and setter for maximum processing progress (used on server side).
	 *
	 * @param getter 最大进度值获取器 / The max progress value getter
	 * @param setter 最大进度值设置器 / The max progress value setter
	 */
	public void setProcessMax(Supplier<Integer> getter,Consumer<Integer> setter) {
		PROCESS_MAX.bind(getter, setter);
	}

	/**
	 * 绑定当前加工进度的只读获取器。
	 * <p>
	 * Binds a read-only getter for the current processing progress.
	 *
	 * @param getter 进度值获取器 / The progress value getter
	 */
	public void setProcess(Supplier<Integer> getter) {
		PROCESS.bind(getter);
	}

	/**
	 * 绑定最大加工进度的只读获取器。
	 * <p>
	 * Binds a read-only getter for the maximum processing progress.
	 *
	 * @param getter 最大进度值获取器 / The max progress value getter
	 */
	public void setProcessMax(Supplier<Integer> getter) {
		PROCESS_MAX.bind(getter);
	}

}
