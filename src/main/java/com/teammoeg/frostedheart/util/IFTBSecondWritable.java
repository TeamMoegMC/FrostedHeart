package com.teammoeg.frostedheart.util;

import net.minecraft.network.PacketBuffer;

public interface IFTBSecondWritable {
    void write2(PacketBuffer pb, long now);
}
