package com.teammoeg.chorda.capability.nbt;

import com.teammoeg.chorda.util.io.NBTSerializable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class CNBTCapabilityProvider<C extends NBTSerializable> implements ICapabilitySerializable<CompoundTag>{
	LazyOptional<C> lazyCap;
	CNBTCapabilityType<C> capability;
	public CNBTCapabilityProvider(CNBTCapabilityType<C> capability) {
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
