package com.teammoeg.frostedheart.base.network;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public abstract class NBTMessage implements FHMessage{
	private CompoundNBT tag;

	public NBTMessage(PacketBuffer buffer) {
		this(buffer.readNbt());
	}
	public NBTMessage(CompoundNBT tag) {
		super();
		this.tag = tag;
	}

	@Override
	public void encode(PacketBuffer buffer) {
		buffer.writeNbt(tag);
	}


	public CompoundNBT getTag() {
		return tag;
	}


	public void setTag(CompoundNBT tag) {
		this.tag = tag;
	}

}
