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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;

import com.teammoeg.chorda.capability.CapabilityStored;
import com.teammoeg.chorda.io.CodecUtil;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 基于Codec序列化的能力提供者。
 * 使用Mojang的{@link com.mojang.serialization.Codec}系统进行序列化和反序列化，
 * 将能力数据持久化为NBT Tag。反序列化时会创建全新的对象实例替换旧的。
 * <p>
 * Codec-based serialization capability provider.
 * Uses Mojang's {@link com.mojang.serialization.Codec} system for serialization and deserialization,
 * persisting capability data as NBT Tags. During deserialization, creates a brand new object instance
 * to replace the old one.
 *
 * @param <T> 能力的具体类型 / The concrete type of the capability
 */
public class CodecCapabilityProvider<T> implements ICapabilitySerializable<Tag>,CapabilityStored<T> {
	/** 能力实例的延迟加载可选值 / Lazily loaded optional value of the capability instance */
	LazyOptional<T> lazyCap;
	/** 关联的Codec能力类型定义 / The associated Codec capability type definition */
	CodecCapabilityType<T> capability;
	/**
	 * 使用指定的Codec能力类型构造提供者，并创建初始能力实例。
	 * <p>
	 * Constructs a provider with the specified Codec capability type and creates the initial capability instance.
	 *
	 * @param capability Codec能力类型定义 / The Codec capability type definition
	 */
	public CodecCapabilityProvider(CodecCapabilityType<T> capability) {
		this.capability=capability;
		this.lazyCap=capability.createCapability();
	}
	/**
	 * 获取请求的能力。仅当请求的能力与此提供者的能力匹配时返回。
	 * <p>
	 * Gets the requested capability. Only returns when the requested capability matches this provider's capability.
	 *
	 * @param cap 请求的能力 / The requested capability
	 * @param side 访问方向 / The access direction
	 * @param <A> 请求的能力类型 / The requested capability type
	 * @return 包含能力实例的LazyOptional，或空 / A LazyOptional containing the capability instance, or empty
	 */
	@Override
	public <A> LazyOptional<A> getCapability(Capability<A> cap, Direction side) {
		if(cap==capability.capability())
			return lazyCap.cast();
		return LazyOptional.empty();
	}

	/**
	 * 使用Codec将能力数据序列化为NBT Tag。
	 * <p>
	 * Serializes the capability data to an NBT Tag using the Codec.
	 *
	 * @return 序列化后的NBT Tag / The serialized NBT Tag
	 */
	@Override
	public Tag serializeNBT() {
		return lazyCap.map(t->CodecUtil.encodeOrThrow(capability.codec().encodeStart(NbtOps.INSTANCE, t))).orElseGet(CompoundTag::new);
	}

	/**
	 * 使用Codec从NBT Tag反序列化能力数据。
	 * 如果NBT非空，旧的LazyOptional会被失效并替换为包含新解码对象的新实例。
	 * <p>
	 * Deserializes capability data from an NBT Tag using the Codec.
	 * If the NBT is non-empty, the old LazyOptional is invalidated and replaced with
	 * a new instance containing the freshly decoded object.
	 *
	 * @param nbt 要反序列化的NBT Tag / The NBT Tag to deserialize from
	 */
	@Override
	public void deserializeNBT(Tag nbt) {
		if(nbt.getId()!=Tag.TAG_COMPOUND||!((CompoundTag)nbt).isEmpty()){
			lazyCap.invalidate();
			T obj=CodecUtil.decodeOrThrow(capability.codec().decode(NbtOps.INSTANCE, nbt));
			lazyCap=LazyOptional.of(()->obj);
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Capability<T> capability() {
		return capability.capability();
	}

}
