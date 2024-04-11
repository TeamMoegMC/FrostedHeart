package com.teammoeg.frostedheart.base.capability.codec;

import com.teammoeg.frostedheart.util.io.CodecUtil;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class FHCodecCapabilityProvider<T> implements ICapabilitySerializable<INBT> {
	LazyOptional<T> lazyCap;
	FHCodecCapability<T> capability;
	public FHCodecCapabilityProvider(FHCodecCapability<T> capability) {
		this.capability=capability;
		this.lazyCap=capability.createCapability();
	}
	@Override
	public <A> LazyOptional<A> getCapability(Capability<A> cap, Direction side) {
		if(cap==capability.capability())
			return lazyCap.cast();
		return LazyOptional.empty();
	}

	@Override
	public INBT serializeNBT() {
		return lazyCap.map(t->CodecUtil.encodeOrThrow(capability.codec().encodeStart(NBTDynamicOps.INSTANCE, t))).orElseGet(CompoundNBT::new);
	}

	@Override
	public void deserializeNBT(INBT nbt) {
		lazyCap.invalidate();
		T obj=CodecUtil.decodeOrThrow(capability.codec().decode(NBTDynamicOps.INSTANCE, nbt));
		lazyCap=LazyOptional.of(()->obj);
	}

}
