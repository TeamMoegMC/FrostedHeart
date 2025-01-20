package com.teammoeg.chorda.capability.codec;

import com.teammoeg.chorda.util.io.CodecUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class CCodecCapabilityProvider<T> implements ICapabilitySerializable<Tag> {
	LazyOptional<T> lazyCap;
	CCodecCapabilityType<T> capability;
	public CCodecCapabilityProvider(CCodecCapabilityType<T> capability) {
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
	public Tag serializeNBT() {
		return lazyCap.map(t->CodecUtil.encodeOrThrow(capability.codec().encodeStart(NbtOps.INSTANCE, t))).orElseGet(CompoundTag::new);
	}

	@Override
	public void deserializeNBT(Tag nbt) {
		lazyCap.invalidate();
		T obj=CodecUtil.decodeOrThrow(capability.codec().decode(NbtOps.INSTANCE, nbt));
		lazyCap=LazyOptional.of(()->obj);
	}

}
