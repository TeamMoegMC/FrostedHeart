package com.teammoeg.chorda.io;

import net.minecraft.network.FriendlyByteBuf;

public interface PacketWritable {

	@Deprecated void write(FriendlyByteBuf buffer);

}