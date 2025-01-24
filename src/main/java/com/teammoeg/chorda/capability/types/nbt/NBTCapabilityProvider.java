package com.teammoeg.chorda.capability.types.nbt;

import net.minecraft.nbt.CompoundTag;

import com.teammoeg.chorda.io.NBTSerializable;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class NBTCapabilityProvider<C extends NBTSerializable> implements ICapabilitySerializable<CompoundTag>{
	LazyOptional<C> lazyCap;
	NBTCapabilityType<C> capability;
	public NBTCapabilityProvider(NBTCapabilityType<C> capability) {
		super();
		this.capability = capability;
		this.lazyCap=capability.createCapability();
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==capability.capability()) {
			return lazyCap.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public CompoundTag serializeNBT() {
		return lazyCap.map(NBTSerializable::serializeNBT).orElseGet(CompoundTag::new);
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		lazyCap.ifPresent(c->c.deserializeNBT(nbt));
	}

}
