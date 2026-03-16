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

import org.objectweb.asm.Type;

import com.teammoeg.chorda.capability.CapabilityStored;
import com.teammoeg.chorda.capability.types.CapabilityType;
import com.teammoeg.chorda.mixin.CapabilityManagerAccess;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * 非持久化（瞬态）能力类型定义。
 * 用于注册那些有特殊数据存储方式的能力，例如物品NBT、方块实体等自行管理数据的场景。
 * 此类型不负责序列化/反序列化，对应的能力类不应实现INBTSerializable。
 * 能力数据仅在运行时存在，不会被保存到磁盘。
 * <p>
 * Non-persistent (transient) capability type definition.
 * Used to register capabilities that have special data storage mechanisms, such as item NBT
 * or block entities that manage their own data.
 * This type does not handle serialization/deserialization; the corresponding capability class
 * should not implement INBTSerializable.
 * Capability data only exists at runtime and is not saved to disk.
 *
 * @param <C> 能力的具体类型 / The concrete type of the capability
 */
public class TransientCapability<C> implements CapabilityType<C>,CapabilityStored<C> {
	/** 能力类的Class对象 / The Class object of the capability */
	private Class<C> capClass;
	/** Forge能力实例，注册后可用 / The Forge capability instance, available after registration */
	private Capability<C> capability;

	/**
	 * 构造一个瞬态能力类型。
	 * <p>
	 * Constructs a transient capability type.
	 *
	 * @param capClass 能力类的Class对象 / The Class object of the capability
	 */
	public TransientCapability(Class<C> capClass) {
		super();
		this.capClass = capClass;
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
	 * 使用给定的工厂方法创建一个瞬态能力提供者。
	 * <p>
	 * Creates a transient capability provider with the given factory method.
	 *
	 * @param factory 能力实例的工厂方法 / The factory method for creating capability instances
	 * @return 新的瞬态能力提供者 / A new transient capability provider
	 */
	public TransientCapabilityProvider<C> provider(NonNullSupplier<C> factory) {
		return new TransientCapabilityProvider<>(this,factory);
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
	@Override
    public Capability<C> capability() {
		return capability;
	}
	/** {@inheritDoc} */
	public Class<C> getCapClass() {
		return capClass;
	}
}
