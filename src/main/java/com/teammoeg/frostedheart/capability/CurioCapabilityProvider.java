package com.teammoeg.frostedheart.capability;

import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CurioCapabilityProvider implements ICapabilityProvider{
	LazyOptional<ICurio> lazyCap;


	public CurioCapabilityProvider(NonNullSupplier<ICurio> lazyCap) {
		super();
		this.lazyCap = LazyOptional.of(lazyCap);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==CuriosCapability.ITEM) {
			return lazyCap.cast();
		}
		return LazyOptional.empty();
	}

}
