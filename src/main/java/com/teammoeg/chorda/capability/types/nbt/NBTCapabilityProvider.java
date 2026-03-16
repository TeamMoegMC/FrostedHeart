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

import net.minecraft.nbt.CompoundTag;

import com.teammoeg.chorda.capability.CapabilityStored;
import com.teammoeg.chorda.io.NBTSerializable;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 基于NBT序列化的能力提供者。
 * 使用{@link NBTSerializable}接口进行序列化和反序列化，将能力数据持久化为{@link CompoundTag}。
 * 与{@link CodecCapabilityProvider}不同，反序列化时在现有对象上原地更新，而非替换。
 * <p>
 * NBT-based serialization capability provider.
 * Uses the {@link NBTSerializable} interface for serialization and deserialization,
 * persisting capability data as a {@link CompoundTag}.
 * Unlike {@link CodecCapabilityProvider}, deserialization updates the existing object in-place
 * rather than replacing it.
 *
 * @param <C> 能力的具体类型，必须实现NBTSerializable / The concrete type of the capability, must implement NBTSerializable
 */
public class NBTCapabilityProvider<C extends NBTSerializable> implements ICapabilitySerializable<CompoundTag>,CapabilityStored<C>{
	/** 能力实例的延迟加载可选值 / Lazily loaded optional value of the capability instance */
	LazyOptional<C> lazyCap;
	/** 关联的NBT能力类型定义 / The associated NBT capability type definition */
	NBTCapabilityType<C> capability;
	/**
	 * 使用指定的NBT能力类型构造提供者，并创建初始能力实例。
	 * <p>
	 * Constructs a provider with the specified NBT capability type and creates the initial capability instance.
	 *
	 * @param capability NBT能力类型定义 / The NBT capability type definition
	 */
	public NBTCapabilityProvider(NBTCapabilityType<C> capability) {
		super();
		this.capability = capability;
		this.lazyCap=capability.createCapability();
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

	/**
	 * 将能力数据序列化为CompoundTag。委托给能力实例的{@link NBTSerializable#serializeNBT()}方法。
	 * <p>
	 * Serializes the capability data to a CompoundTag. Delegates to the capability instance's
	 * {@link NBTSerializable#serializeNBT()} method.
	 *
	 * @return 序列化后的CompoundTag / The serialized CompoundTag
	 */
	@Override
	public CompoundTag serializeNBT() {
		return lazyCap.map(NBTSerializable::serializeNBT).orElseGet(CompoundTag::new);
	}

	/**
	 * 从CompoundTag反序列化能力数据。如果NBT非空，则在现有能力实例上原地更新。
	 * <p>
	 * Deserializes capability data from a CompoundTag. If the NBT is non-empty,
	 * updates the existing capability instance in-place.
	 *
	 * @param nbt 要反序列化的CompoundTag / The CompoundTag to deserialize from
	 */
	@Override
	public void deserializeNBT(CompoundTag nbt) {
		if(!nbt.isEmpty())
			lazyCap.ifPresent(c->c.deserializeNBT(nbt));
	}

	/** {@inheritDoc} */
	@Override
	public Capability<C> capability() {
		return capability.capability();
	}

}
