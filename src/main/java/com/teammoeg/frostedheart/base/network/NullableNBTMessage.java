package com.teammoeg.frostedheart.base.network;

import java.util.Optional;

import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public abstract class NullableNBTMessage extends NBTMessage {

	public NullableNBTMessage(FriendlyByteBuf buffer) {
		this(SerializeUtil.readOptional(buffer, FriendlyByteBuf::readNbt));
	}

	public NullableNBTMessage(Optional<CompoundTag> tag) {
		super(tag.orElse(null));
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeOptional2(buffer, this.getTag(),FriendlyByteBuf::writeNbt);
	}

}
