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

package com.teammoeg.chorda.capability.types.codec;

import org.objectweb.asm.Type;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.capability.CapabilityStored;
import com.teammoeg.chorda.capability.types.CapabilityType;
import com.teammoeg.chorda.mixin.CapabilityManagerAccess;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * 基于Codec序列化的能力类型定义。
 * 使用Mojang的{@link Codec}系统进行序列化和反序列化。
 * 持有能力的Class、工厂方法和Codec，负责注册能力并提供能力提供者实例。
 * <p>
 * Codec-serialized capability type definition.
 * Uses Mojang's {@link Codec} system for serialization and deserialization.
 * Holds the capability's Class, factory method, and Codec; responsible for registering
 * the capability and providing capability provider instances.
 *
 * @param <C> 能力的具体类型 / The concrete type of the capability
 */
public class CodecCapabilityType<C> implements CapabilityType<C>,CapabilityStored<C> {
	/** 能力类的Class对象 / The Class object of the capability */
	private Class<C> capClass;
	/** Forge能力实例，注册后可用 / The Forge capability instance, available after registration */
	private Capability<C> capability;
	/** 能力实例的工厂方法 / Factory method for creating capability instances */
	private NonNullSupplier<C> factory;
	/** 用于序列化/反序列化的Codec / The Codec used for serialization/deserialization */
	private Codec<C> codec;

	/**
	 * 构造一个Codec能力类型。
	 * <p>
	 * Constructs a Codec capability type.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 * @param factory 能力实例的工厂方法 / The factory method for creating capability instances
	 * @param codec 用于序列化/反序列化的Codec / The Codec for serialization/deserialization
	 */
	public CodecCapabilityType(Class<C> capClass, NonNullSupplier<C> factory, Codec<C> codec) {
		super();
		this.capClass = capClass;
		this.factory = factory;
		this.codec = codec;
	}
	/**
	 * 通过Mixin访问Forge的CapabilityManager内部，获取并保存对应的Capability实例。
	 * <p>
	 * Obtains and stores the corresponding Capability instance by accessing Forge's CapabilityManager
	 * internals via Mixin.
	 */
	@SuppressWarnings("unchecked")
	public void register() {
        capability=(Capability<C>) ((CapabilityManagerAccess)(Object)CapabilityManager.INSTANCE).getProviders().get(Type.getInternalName(capClass).intern());
	}
	/**
	 * 创建一个新的Codec能力提供者实例。
	 * <p>
	 * Creates a new Codec capability provider instance.
	 *
	 * @return 新的Codec能力提供者 / A new Codec capability provider
	 */
	public CodecCapabilityProvider<C> provider() {
		return new CodecCapabilityProvider<>(this);
	}
	/**
	 * 使用工厂方法创建一个新的能力实例，包装在LazyOptional中。
	 * <p>
	 * Creates a new capability instance using the factory method, wrapped in a LazyOptional.
	 *
	 * @return 包含新能力实例的LazyOptional / A LazyOptional containing the new capability instance
	 */
	LazyOptional<C> createCapability(){
		return LazyOptional.of(factory);
	}
	/**
	 * 从给定对象获取此能力类型的LazyOptional。
	 * 如果对象实现了ICapabilityProvider，则查询其能力；否则返回空。
	 * <p>
	 * Gets a LazyOptional of this capability type from the given object.
	 * If the object implements ICapabilityProvider, queries its capability; otherwise returns empty.
	 *
	 * @param cap 要查询能力的对象 / The object to query the capability from
	 * @return 包含能力实例的LazyOptional，或空 / A LazyOptional containing the capability instance, or empty
	 */
	public LazyOptional<C> getCapability(Object cap) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability);
		return LazyOptional.empty();
	}
    /** {@inheritDoc} */
    public Capability<C> capability() {
		return capability;
	}
    /**
     * 获取此能力类型使用的Codec。
     * <p>
     * Gets the Codec used by this capability type.
     *
     * @return 序列化/反序列化用的Codec / The Codec for serialization/deserialization
     */
    public Codec<C> codec(){
    	return codec;
    }
	/** {@inheritDoc} */
	public Class<C> getCapClass() {
		return capClass;
	}
}
