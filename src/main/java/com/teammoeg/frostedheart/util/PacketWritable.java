package com.teammoeg.frostedheart.util;

import net.minecraft.network.PacketBuffer;

public interface PacketWritable {

	void write(PacketBuffer buffer);

}