package com.teammoeg.frostedheart.base.capability;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class FHCapabilityProvider<C extends NBTSerializable> implements ICapabilitySerializable<CompoundNBT>{
	LazyOptional<C> lazyCap;
	FHNBTCapability<C> capability;
	public FHCapabilityProvider(FHNBTCapability<C> capability) {
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
	public CompoundNBT serializeNBT() {
		return lazyCap.map(NBTSerializable::serializeNBT).orElseGet(CompoundNBT::new);
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt) {
		lazyCap.ifPresent(c->c.deserializeNBT(nbt));
	}

}
