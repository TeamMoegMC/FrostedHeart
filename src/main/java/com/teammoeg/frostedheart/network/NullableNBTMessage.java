package com.teammoeg.frostedheart.network;

import java.util.Optional;

import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public abstract class NullableNBTMessage extends NBTMessage {

	public NullableNBTMessage(PacketBuffer buffer) {
		this(SerializeUtil.readOptional(buffer, PacketBuffer::readCompoundTag));
	}

	public NullableNBTMessage(Optional<CompoundNBT> tag) {
		super(tag.orElse(null));
	}

	@Override
	public void encode(PacketBuffer buffer) {
		SerializeUtil.writeOptional2(buffer, this.getTag(),PacketBuffer::writeCompoundTag);
	}

}
