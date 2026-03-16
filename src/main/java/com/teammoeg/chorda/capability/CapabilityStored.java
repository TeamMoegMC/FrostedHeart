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

package com.teammoeg.chorda.capability;

import net.minecraftforge.common.capabilities.Capability;

/**
 * 标记接口，表示一个对象存储了对某个Forge Capability的引用。
 * 实现此接口的类可以通过{@link #capability()}方法获取其关联的能力实例。
 * 通常与{@link net.minecraftforge.common.capabilities.ICapabilityProvider}配合使用，
 * 使提供者能够自描述其所提供的能力类型。
 * <p>
 * Marker interface indicating that an object stores a reference to a Forge Capability.
 * Classes implementing this interface can retrieve their associated capability instance
 * via the {@link #capability()} method.
 * Typically used alongside {@link net.minecraftforge.common.capabilities.ICapabilityProvider}
 * to allow a provider to self-describe which capability type it provides.
 *
 * @param <T> 能力的类型 / The type of the capability
 */
public interface CapabilityStored<T> {
	/**
	 * 获取此对象关联的Forge Capability实例。
	 * <p>
	 * Gets the Forge Capability instance associated with this object.
	 *
	 * @return Forge能力实例 / The Forge capability instance
	 */
	Capability<T> capability();
}
