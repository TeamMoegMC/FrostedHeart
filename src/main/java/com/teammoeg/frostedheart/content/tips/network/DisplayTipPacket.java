package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class DisplayTipPacket implements FHMessage {
    private final String ID;

    public DisplayTipPacket(PacketBuffer buffer) {
        ID = buffer.readString(Short.MAX_VALUE);
    }

    public DisplayTipPacket(String ID) {
        this.ID = ID;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeString(this.ID);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TipDisplayManager.displayTip(ID, false));
        context.get().setPacketHandled(true);
    }
}
