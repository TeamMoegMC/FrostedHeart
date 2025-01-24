package com.teammoeg.chorda.capability.types.nonpresistent;

import org.objectweb.asm.Type;

import com.teammoeg.chorda.capability.types.CapabilityType;
import com.teammoeg.frostedheart.mixin.forge.CapabilityManagerAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * Non persistent capability type.
 * Used to register capablity that have special data storage like item nbt, block entities...
 * Coresponding capability class should not implement INBTSerializable.
 * */
public class TransientCapability<C> implements CapabilityType {
	private Class<C> capClass;
	private Capability<C> capability;

	public TransientCapability(Class<C> capClass) {
		super();
		this.capClass = capClass;
	}
	@SuppressWarnings("unchecked")
	public void register() {
        capability=(Capability<C>) ((CapabilityManagerAccess)(Object)CapabilityManager.INSTANCE).getProviders().get(Type.getInternalName(capClass).intern());
	}
	public ICapabilityProvider provider(NonNullSupplier<C> factory) {
		return new TransientCapabilityProvider<>(this,factory);
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
