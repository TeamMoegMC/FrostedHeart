package com.teammoeg.frostedheart.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public class FHCapabilityProvider<C extends INBTSerializable<CompoundNBT>> implements ICapabilitySerializable<CompoundNBT>{
	LazyOptional<C> lazyCap;
	Capability<C> capability;
	public FHCapabilityProvider(Capability<C> capability,NonNullSupplier<C> cap) {
		super();
		this.capability = capability;
		this.lazyCap=LazyOptional.of(cap);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==capability) {
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
