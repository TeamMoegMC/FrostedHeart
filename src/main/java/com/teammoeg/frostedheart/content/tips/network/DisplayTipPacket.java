package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DisplayTipPacket(String id) implements CMessage {

    public DisplayTipPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(Short.MAX_VALUE));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.id);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TipManager.INSTANCE.display().general(id));
        context.get().setPacketHandled(true);
    }
}
