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

package com.teammoeg.chorda.capability.types;

/**
 * 能力类型的基础接口。
 * 定义了所有能力类型（NBT序列化、Codec序列化、瞬态）必须实现的通用契约。
 * 每种能力类型负责管理其能力类的注册和类信息的提供。
 * <p>
 * Base interface for capability types.
 * Defines the common contract that all capability types (NBT-serialized, Codec-serialized,
 * transient) must implement. Each capability type is responsible for managing the registration
 * of its capability class and providing class information.
 *
 * @param <T> 能力的具体类型 / The concrete type of the capability
 */
public interface CapabilityType<T> {
	/**
	 * 将此能力类型注册到Forge能力系统中。
	 * 在{@link net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent}事件处理期间被调用。
	 * <p>
	 * Registers this capability type with the Forge capability system.
	 * Called during the {@link net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent} event handling.
	 */
	public void register();
	/**
	 * 获取此能力类型对应的Class对象。
	 * <p>
	 * Gets the Class object corresponding to this capability type.
	 *
	 * @return 能力类的Class对象 / The Class object of the capability
	 */
	public Class<T> getCapClass();

}
