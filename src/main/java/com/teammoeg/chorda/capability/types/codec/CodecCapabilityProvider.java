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

public class CodecCapabilityProvider<T> implements ICapabilitySerializable<Tag>,CapabilityStored<T> {
	LazyOptional<T> lazyCap;
	CodecCapabilityType<T> capability;
	public CodecCapabilityProvider(CodecCapabilityType<T> capability) {
		this.capability=capability;
		this.lazyCap=capability.createCapability();
	}
	@Override
	public <A> LazyOptional<A> getCapability(Capability<A> cap, Direction side) {
		if(cap==capability.capability())
			return lazyCap.cast();
		return LazyOptional.empty();
	}

	@Override
	public Tag serializeNBT() {
		return lazyCap.map(t->CodecUtil.encodeOrThrow(capability.codec().encodeStart(NbtOps.INSTANCE, t))).orElseGet(CompoundTag::new);
	}

	@Override
	public void deserializeNBT(Tag nbt) {
		if(nbt.getId()!=Tag.TAG_COMPOUND||!((CompoundTag)nbt).isEmpty()){
			lazyCap.invalidate();
			T obj=CodecUtil.decodeOrThrow(capability.codec().decode(NbtOps.INSTANCE, nbt));
			lazyCap=LazyOptional.of(()->obj);
		}
	}
	@Override
	public Capability<T> capability() {
		return capability.capability();
	}

}
