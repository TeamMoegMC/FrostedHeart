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

package com.teammoeg.chorda.capability.types.nbt;

import org.objectweb.asm.Type;

import com.teammoeg.chorda.capability.CapabilityStored;
import com.teammoeg.chorda.capability.types.CapabilityType;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.mixin.CapabilityManagerAccess;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * 基于NBT序列化的能力类型定义。
 * 使用{@link NBTSerializable}接口进行数据持久化。
 * 持有能力的Class和工厂方法，负责注册能力并提供能力提供者实例。
 * <p>
 * NBT-serialized capability type definition.
 * Uses the {@link NBTSerializable} interface for data persistence.
 * Holds the capability's Class and factory method; responsible for registering
 * the capability and providing capability provider instances.
 *
 * @param <C> 能力的具体类型，必须实现NBTSerializable / The concrete type of the capability, must implement NBTSerializable
 */
public class NBTCapabilityType<C extends NBTSerializable> implements CapabilityType<C>,CapabilityStored<C> {
	/** 能力类的Class对象 / The Class object of the capability */
	private Class<C> capClass;
	/** Forge能力实例，注册后可用 / The Forge capability instance, available after registration */
	private Capability<C> capability;
	/** 能力实例的工厂方法 / Factory method for creating capability instances */
	private NonNullSupplier<C> factory;
	/**
	 * 构造一个NBT能力类型。
	 * <p>
	 * Constructs an NBT capability type.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 * @param factory 能力实例的工厂方法 / The factory method for creating capability instances
	 */
	public NBTCapabilityType(Class<C> capClass, NonNullSupplier<C> factory) {
		super();
		this.capClass = capClass;
		this.factory = factory;
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
	 * 创建一个新的NBT能力提供者实例。
	 * <p>
	 * Creates a new NBT capability provider instance.
	 *
	 * @return 新的NBT能力提供者 / A new NBT capability provider
	 */
	public NBTCapabilityProvider<C> provider() {
		return new NBTCapabilityProvider<>(this);
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
	 * 从给定对象获取此能力类型的LazyOptional（无方向）。
	 * 如果对象实现了ICapabilityProvider，则查询其能力；否则打印堆栈跟踪并返回空。
	 * <p>
	 * Gets a LazyOptional of this capability type from the given object (directionless).
	 * If the object implements ICapabilityProvider, queries its capability;
	 * otherwise prints a stack trace and returns empty.
	 *
	 * @param cap 要查询能力的对象 / The object to query the capability from
	 * @return 包含能力实例的LazyOptional，或空 / A LazyOptional containing the capability instance, or empty
	 */
	public LazyOptional<C> getCapability(Object cap) {

		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability);
		new Exception().printStackTrace();
		return LazyOptional.empty();
	}
	/**
	 * 从给定对象获取此能力类型的LazyOptional（指定方向）。
	 * 如果对象实现了ICapabilityProvider，则查询其能力；否则返回空。
	 * <p>
	 * Gets a LazyOptional of this capability type from the given object (with direction).
	 * If the object implements ICapabilityProvider, queries its capability; otherwise returns empty.
	 *
	 * @param cap 要查询能力的对象 / The object to query the capability from
	 * @param dir 访问方向 / The access direction
	 * @return 包含能力实例的LazyOptional，或空 / A LazyOptional containing the capability instance, or empty
	 */
	public LazyOptional<C> getCapability(Object cap,Direction dir) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability,dir);
		return LazyOptional.empty();
	}
    /** {@inheritDoc} */
    public Capability<C> capability() {
		return capability;
	}
    /**
     * 检查给定的Capability是否与此能力类型匹配。
     * <p>
     * Checks whether the given Capability matches this capability type.
     *
     * @param cap 要检查的能力 / The capability to check
     * @return 如果匹配则为true / true if it matches
     */
    public boolean isCapability(Capability<?> cap) {
		return capability==cap;
	}
	/** {@inheritDoc} */
	public Class<C> getCapClass() {
		return capClass;
	}
}
