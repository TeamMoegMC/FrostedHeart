package com.teammoeg.frostedheart.util;

import net.minecraft.network.PacketBuffer;

public interface PacketWritable {

	@Deprecated void write(PacketBuffer buffer);

}