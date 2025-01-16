package com.teammoeg.chorda.capability.nbt;

import org.objectweb.asm.Type;

import com.teammoeg.chorda.capability.CCapability;
import com.teammoeg.frostedheart.mixin.forge.CapabilityManagerAccess;
import com.teammoeg.chorda.util.io.NBTSerializable;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * Basic nbt capability type
 * 
 * */
public class CNBTCapability<C extends NBTSerializable> implements CCapability {
	private Class<C> capClass;
	private Capability<C> capability;
	private NonNullSupplier<C> factory;
	public CNBTCapability(Class<C> capClass, NonNullSupplier<C> factory) {
		super();
		this.capClass = capClass;
		this.factory = factory;
	}
	@SuppressWarnings("unchecked")
	public void register() {
        capability=(Capability<C>) ((CapabilityManagerAccess)(Object)CapabilityManager.INSTANCE).getProviders().get(Type.getInternalName(capClass).intern());
	}
	public ICapabilityProvider provider() {
		return new CNBTCapabilityProvider<>(this);
	}
	LazyOptional<C> createCapability(){
		return LazyOptional.of(factory);
	}
	public LazyOptional<C> getCapability(Object cap) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability);
		return LazyOptional.empty();
	}
	public LazyOptional<C> getCapability(Object cap,Direction dir) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability,dir);
		return LazyOptional.empty();
	}
    public Capability<C> capability() {
		return capability;
	}
    public boolean isCapability(Capability<?> cap) {
		return capability==cap;
	}
	public Class<C> getCapClass() {
		return capClass;
	}
}
