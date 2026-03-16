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

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.capability.types.nonpresistent.TransientCapability;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * 能力分发的构建器，用于在{@code initCapabilities}中简化多个能力的注册与分发。
 * 每个提供者应只包含一种能力类型，每种能力应只有一个提供者。
 * 使用构建器模式，通过链式调用{@link #add}方法添加能力，最后调用{@link #build()}生成分发器。
 * <p>
 * Builder for capability dispatching, simplifying registration and dispatch of multiple capabilities
 * in {@code initCapabilities}.
 * Each provider should only have a single capability type, and each capability should only have a single provider.
 * Uses the builder pattern with chained {@link #add} calls, finalized by {@link #build()} to produce a dispatcher.
 */
public class CapabilityDispatchBuilder {
	/**
	 * 能力分发器的内部实现，持有能力到提供者的映射，负责将能力请求路由到对应的提供者。
	 * <p>
	 * Internal implementation of the capability dispatcher. Holds the mapping from capabilities
	 * to providers and routes capability requests to the appropriate provider.
	 */
	public static class CapabilityDispatcher implements ICapabilityProvider{
		/** 能力到提供者的映射 / Mapping from capabilities to their providers */
		Map<Capability<?>,ICapabilityProvider> caps;

		/**
		 * 使用给定的能力映射构造分发器。
		 * <p>
		 * Constructs a dispatcher with the given capability mapping.
		 *
		 * @param caps 能力到提供者的映射 / Mapping from capabilities to their providers
		 */
		private CapabilityDispatcher(Map<Capability<?>, ICapabilityProvider> caps) {
			super();
			this.caps = caps;
		}

		/**
		 * 根据请求的能力类型，将请求分发到对应的提供者。
		 * <p>
		 * Dispatches a capability request to the corresponding provider based on the requested capability type.
		 *
		 * @param cap 请求的能力 / The requested capability
		 * @param side 访问方向，可为null / The access direction, may be null
		 * @param <T> 能力类型 / The capability type
		 * @return 包含能力实例的LazyOptional，如果不支持则为空 / A LazyOptional containing the capability instance, or empty if unsupported
		 */
		@Override
		public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
			ICapabilityProvider prov=caps.get(cap);
			if(prov==null)
				return LazyOptional.empty();
			return prov.getCapability(cap,side);
		}

	}
	/** 构建过程中收集的能力到提供者的映射 / Capability-to-provider mapping collected during building */
	Map<Capability<?>,ICapabilityProvider> caps=new IdentityHashMap<>();
	/**
	 * 创建一个新的构建器实例。
	 * <p>
	 * Creates a new builder instance.
	 *
	 * @return 新的构建器 / A new builder instance
	 */
	public static CapabilityDispatchBuilder builder() {
		return new CapabilityDispatchBuilder();
	}
	/**
	 * 使用非空供应器添加一个能力。供应器将被包装为LazyOptional。
	 * <p>
	 * Adds a capability with a non-null supplier. The supplier will be wrapped in a LazyOptional.
	 *
	 * @param cap 要添加的能力 / The capability to add
	 * @param provider 能力实例的供应器 / The supplier of the capability instance
	 * @param <C> 能力类型 / The capability type
	 * @return 当前构建器，用于链式调用 / This builder for chaining
	 */
	public <C> CapabilityDispatchBuilder add(Capability<C> cap,NonNullSupplier<C> provider) {
		add(cap,LazyOptional.of(provider));
		return this;
	}
	/**
	 * 使用LazyOptional添加一个能力。将创建一个匿名ICapabilityProvider来包装它。
	 * <p>
	 * Adds a capability with a LazyOptional. Creates an anonymous ICapabilityProvider to wrap it.
	 *
	 * @param cap 要添加的能力 / The capability to add
	 * @param provider 包含能力实例的LazyOptional / The LazyOptional containing the capability instance
	 * @param <C> 能力类型 / The capability type
	 * @return 当前构建器，用于链式调用 / This builder for chaining
	 */
	public <C> CapabilityDispatchBuilder add(Capability<C> cap,LazyOptional<C> provider) {
		add(cap, new ICapabilityProvider() {
			@Override
			public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capin, @Nullable Direction side) {
				return capin==cap?provider.cast():LazyOptional.empty();
			}
		});
		return this;
	}
	/**
	 * 使用ICapabilityProvider直接添加一个能力。
	 * <p>
	 * Adds a capability directly with an ICapabilityProvider.
	 *
	 * @param cap 要添加的能力 / The capability to add
	 * @param provider 能力提供者 / The capability provider
	 * @param <T> 能力类型 / The capability type
	 * @return 当前构建器，用于链式调用 / This builder for chaining
	 */
	public <T> CapabilityDispatchBuilder add(Capability<T> cap,ICapabilityProvider provider) {
		caps.put(cap, provider);
		return this;
	}
	/**
	 * 使用CapabilityStored接口从中提取Capability，然后添加提供者。
	 * <p>
	 * Adds a capability by extracting the Capability from a CapabilityStored interface, then adding the provider.
	 *
	 * @param cap 存储了能力引用的CapabilityStored / The CapabilityStored holding the capability reference
	 * @param provider 能力提供者 / The capability provider
	 * @param <T> 能力类型 / The capability type
	 * @return 当前构建器，用于链式调用 / This builder for chaining
	 */
	public <T> CapabilityDispatchBuilder add(CapabilityStored<T> cap,ICapabilityProvider provider) {
		add(cap.capability(),provider);
		return this;
	}
	
	/**
	 * 为瞬态能力添加一个供应器。会自动通过TransientCapability创建对应的提供者。
	 * <p>
	 * Adds a supplier for a transient capability. Automatically creates the corresponding provider
	 * via TransientCapability.
	 *
	 * @param cap 瞬态能力类型 / The transient capability type
	 * @param provider 能力实例的供应器 / The supplier of the capability instance
	 * @param <T> 能力类型 / The capability type
	 * @return 当前构建器，用于链式调用 / This builder for chaining
	 */
	public <T> CapabilityDispatchBuilder add(TransientCapability<T> cap,NonNullSupplier<T> provider) {
		add(cap,cap.provider(provider));
		return this;
	}
	/**
	 * 添加一个同时实现了ICapabilityProvider和CapabilityStored的提供者。
	 * 能力引用会自动从提供者中提取。
	 * <p>
	 * Adds a provider that implements both ICapabilityProvider and CapabilityStored.
	 * The capability reference is automatically extracted from the provider.
	 *
	 * @param provider 同时作为提供者和能力存储的对象 / The object serving as both provider and capability store
	 * @param <T> 能力类型 / The capability type
	 * @param <C> 提供者类型，必须同时实现ICapabilityProvider和CapabilityStored / The provider type, must implement both ICapabilityProvider and CapabilityStored
	 * @return 当前构建器，用于链式调用 / This builder for chaining
	 */
	public <T,C extends ICapabilityProvider&CapabilityStored<T>> CapabilityDispatchBuilder add(C provider) {
		add(provider.capability(),provider);
		return this;
	}
	/**
	 * 构建并返回能力分发器。
	 * <p>
	 * Builds and returns the capability dispatcher.
	 *
	 * @return 配置好的能力分发器 / The configured capability dispatcher
	 */
	public CapabilityDispatcher build() {
		return new CapabilityDispatcher(caps);
	}
}
