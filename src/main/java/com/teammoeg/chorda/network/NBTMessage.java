package com.teammoeg.chorda.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public abstract class NBTMessage implements CMessage {
	private CompoundTag tag;

	public NBTMessage(FriendlyByteBuf buffer) {
		this(buffer.readNbt());
	}
	public NBTMessage(CompoundTag tag) {
		super();
		this.tag = tag;
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeNbt(tag);
	}


	public CompoundTag getTag() {
		return tag;
	}


	public void setTag(CompoundTag tag) {
		this.tag = tag;
	}

}
