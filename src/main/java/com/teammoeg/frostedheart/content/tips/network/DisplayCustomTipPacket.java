package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DisplayCustomTipPacket(Tip tip) implements CMessage {

    public DisplayCustomTipPacket(FriendlyByteBuf buffer) {
        this(Tip.builder("").fromNBT(buffer.readNbt()).build());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        tip.write(buffer);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> TipManager.INSTANCE.display().general(tip));
        ctx.get().setPacketHandled(true);
    }
}
