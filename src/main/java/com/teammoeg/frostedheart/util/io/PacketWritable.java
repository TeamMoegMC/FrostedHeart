package com.teammoeg.frostedheart.util.io;

import net.minecraft.network.FriendlyByteBuf;

public interface PacketWritable {

	@Deprecated void write(FriendlyByteBuf buffer);

}