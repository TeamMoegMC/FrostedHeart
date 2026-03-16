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

package com.teammoeg.chorda.dataholders.team;

import java.util.UUID;
import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;

/**
 * 团队数据闭包，将 {@link TeamDataHolder} 和 {@link SpecialDataType} 绑定在一起，
 * 实现 {@link Supplier} 接口以便延迟获取数据组件。
 * <p>
 * Team data closure that binds a {@link TeamDataHolder} and a {@link SpecialDataType} together,
 * implementing {@link Supplier} for lazy retrieval of the data component.
 *
 * @param <T> 数据组件类型 / the data component type
 * @param team 团队数据持有者 / the team data holder
 * @param type 数据组件的类型定义 / the data component type definition
 */
public record TeamDataClosure<T extends SpecialData>(TeamDataHolder team,SpecialDataType<T> type) implements Supplier<T>{
	/**
	 * 从团队数据持有者中获取指定类型的数据组件。
	 * <p>
	 * Gets the data component of the specified type from the team data holder.
	 *
	 * @return 数据组件实例 / the data component instance
	 */
	public T get() {
		return team.getData(type);
	}
	/**
	 * 获取关联的团队数据持有者的 ID。
	 * <p>
	 * Gets the ID of the associated team data holder.
	 *
	 * @return 团队数据 ID / the team data ID
	 */
	public UUID getId() {
		return team.getId();
	}
}
