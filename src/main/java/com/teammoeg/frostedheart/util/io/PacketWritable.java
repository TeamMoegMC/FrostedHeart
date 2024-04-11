package com.teammoeg.frostedheart.util.io;

import net.minecraft.network.PacketBuffer;

public interface PacketWritable {

	@Deprecated void write(PacketBuffer buffer);

}