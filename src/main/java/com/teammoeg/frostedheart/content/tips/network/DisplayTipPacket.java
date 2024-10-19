package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DisplayTipPacket implements FHMessage {
    private final String ID;

    public DisplayTipPacket(FriendlyByteBuf buffer) {
        ID = buffer.readUtf(Short.MAX_VALUE);
    }

    public DisplayTipPacket(String ID) {
        this.ID = ID;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.ID);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TipDisplayManager.displayTip(ID, false));
        context.get().setPacketHandled(true);
    }
}
