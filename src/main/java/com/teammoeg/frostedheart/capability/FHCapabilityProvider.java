package com.teammoeg.frostedheart.capability;

import com.teammoeg.frostedheart.util.NBTSerializable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public class FHCapabilityProvider<C extends NBTSerializable> implements ICapabilitySerializable<CompoundNBT>{
	LazyOptional<C> lazyCap;
	FHCapability<C> capability;
	public FHCapabilityProvider(FHCapability<C> capability) {
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
		return lazyCap.map(c->c.serializeNBT()).orElseGet(CompoundNBT::new);
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt) {
		lazyCap.ifPresent(c->c.deserializeNBT(nbt));
	}

}
