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

package com.teammoeg.chorda.capability.types.nonpresistent;

import com.teammoeg.chorda.capability.CapabilityStored;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

/**
 * 瞬态（非持久化）能力的提供者。
 * 仅实现{@link ICapabilityProvider}而不实现{@link net.minecraftforge.common.capabilities.ICapabilitySerializable}，
 * 因此不会进行任何序列化或反序列化操作。能力数据仅在运行时存在。
 * <p>
 * Provider for transient (non-persistent) capabilities.
 * Only implements {@link ICapabilityProvider} without {@link net.minecraftforge.common.capabilities.ICapabilitySerializable},
 * so no serialization or deserialization is performed. Capability data only exists at runtime.
 *
 * @param <C> 能力的具体类型 / The concrete type of the capability
 */
public class TransientCapabilityProvider<C> implements ICapabilityProvider,CapabilityStored<C>{
	/** 能力实例的延迟加载可选值 / Lazily loaded optional value of the capability instance */
	LazyOptional<C> lazyCap;
	/** 关联的瞬态能力类型定义 / The associated transient capability type definition */
	TransientCapability<C> capability;
	/**
	 * 使用指定的瞬态能力类型和工厂方法构造提供者。
	 * <p>
	 * Constructs a provider with the specified transient capability type and factory method.
	 *
	 * @param capability 瞬态能力类型定义 / The transient capability type definition
	 * @param factory 能力实例的工厂方法 / The factory method for creating capability instances
	 */
	public TransientCapabilityProvider(TransientCapability<C> capability, NonNullSupplier<C> factory) {
		super();
		this.capability = capability;
		this.lazyCap=LazyOptional.of(factory);
	}
	/**
	 * 获取请求的能力。仅当请求的能力与此提供者的能力匹配时返回。
	 * <p>
	 * Gets the requested capability. Only returns when the requested capability matches this provider's capability.
	 *
	 * @param cap 请求的能力 / The requested capability
	 * @param side 访问方向 / The access direction
	 * @param <T> 请求的能力类型 / The requested capability type
	 * @return 包含能力实例的LazyOptional，或空 / A LazyOptional containing the capability instance, or empty
	 */
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==capability.capability()) {
			return lazyCap.cast();
		}
		return LazyOptional.empty();
	}
	/** {@inheritDoc} */
	@Override
	public Capability<C> capability() {
		return capability.capability();
	}


}
