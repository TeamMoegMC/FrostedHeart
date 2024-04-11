package com.teammoeg.frostedheart.base.capability.codec;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.base.capability.IFHCapability;
import com.teammoeg.frostedheart.mixin.forge.CapabilityManagerAccess;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public class FHCodecCapability<C> implements IFHCapability{
	private Class<C> capClass;
	private Capability<C> capability;
	private NonNullSupplier<C> factory;
	private Codec<C> codec;

	public FHCodecCapability(Class<C> capClass, NonNullSupplier<C> factory, Codec<C> codec) {
		super();
		this.capClass = capClass;
		this.factory = factory;
		this.codec = codec;
	}
	@SuppressWarnings("unchecked")
	public void register() {
        CapabilityManager.INSTANCE.register(capClass, new Capability.IStorage<C>() {
            public void readNBT(Capability<C> capability, C instance, Direction side, INBT nbt) {
                throw new UnsupportedOperationException("Not supported for IStorage read");
            }

            public INBT writeNBT(Capability<C> capability, C instance, Direction side) {
                return CodecUtil.encodeOrThrow(codec.encodeStart(NBTDynamicOps.INSTANCE, instance));
            }
        }, ()->factory.get());
        capability=(Capability<C>) ((CapabilityManagerAccess)(Object)CapabilityManager.INSTANCE).getProviders().get(capClass.getName().intern());
	}
	public ICapabilityProvider provider() {
		return new FHCodecCapabilityProvider<>(this);
	}
	LazyOptional<C> createCapability(){
		return LazyOptional.of(factory);
	}
	public LazyOptional<C> getCapability(Object cap) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability);
		return LazyOptional.empty();
	}
    public Capability<C> capability() {
		return capability;
	}
    public Codec<C> codec(){
    	return codec;
    }
}
