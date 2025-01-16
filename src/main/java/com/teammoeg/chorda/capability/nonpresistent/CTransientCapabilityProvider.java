package com.teammoeg.chorda.capability.nonpresistent;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public class CTransientCapabilityProvider<C> implements ICapabilityProvider{
	LazyOptional<C> lazyCap;
	CTransientCapability<C> capability;
	public CTransientCapabilityProvider(CTransientCapability<C> capability, NonNullSupplier<C> factory) {
		super();
		this.capability = capability;
		this.lazyCap=LazyOptional.of(factory);
	}
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==capability.capability()) {
			return lazyCap.cast();
		}
		return LazyOptional.empty();
	}


}
