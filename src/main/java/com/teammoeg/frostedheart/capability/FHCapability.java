package com.teammoeg.frostedheart.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public class FHCapability<C extends INBTSerializable<CompoundNBT>> {
	private Class<C> capClass;
	private Capability<C> capability;
	private NonNullSupplier<C> factory;
    public Capability<C> capability() {
		return capability;
	}
	public FHCapability(Class<C> capClass, NonNullSupplier<C> factory) {
		super();
		this.capClass = capClass;
		this.factory = factory;
		
	}
	@SuppressWarnings("unchecked")
	public void setup() {
        CapabilityManager.INSTANCE.register(capClass, new Capability.IStorage<C>() {
            public void readNBT(Capability<C> capability, C instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }

            public INBT writeNBT(Capability<C> capability, C instance, Direction side) {
                return instance.serializeNBT();
            }
        }, ()->factory.get());
        capability=(Capability<C>) CapabilityManager.INSTANCE.providers.get(capClass.getName().intern());
    }
	public ICapabilityProvider create() {
		return new FHCapabilityProvider<C>(this);
	}
	LazyOptional<C> createCapability(){
		return LazyOptional.of(factory);
	}
	public LazyOptional<C> getCapability(Object cap) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability);
		return LazyOptional.empty();
	}

}
