package com.teammoeg.frostedheart.base.capability.nonpresistent;

import org.objectweb.asm.Type;

import com.teammoeg.frostedheart.base.capability.IFHCapability;
import com.teammoeg.frostedheart.mixin.forge.CapabilityManagerAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public class FHNPCapability<C> implements IFHCapability{
	private Class<C> capClass;
	private Capability<C> capability;

	public FHNPCapability(Class<C> capClass) {
		super();
		this.capClass = capClass;
	}
	@SuppressWarnings("unchecked")
	public void register() {
        capability=(Capability<C>) ((CapabilityManagerAccess)(Object)CapabilityManager.INSTANCE).getProviders().get(Type.getInternalName(capClass).intern());
	}
	public ICapabilityProvider provider(NonNullSupplier<C> factory) {
		return new FHNPCapabilityProvider<>(this,factory);
	}
	public LazyOptional<C> getCapability(Object cap) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability);
		return LazyOptional.empty();
	}
    public Capability<C> capability() {
		return capability;
	}
	public Class<C> getCapClass() {
		return capClass;
	}
}
