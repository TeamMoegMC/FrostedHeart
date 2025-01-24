package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.tips.Popup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DisplayPopupPacket(String message) implements CMessage {

    public DisplayPopupPacket(Component message) {
        this(Components.getKeyOrElseStr(message));
    }

    public DisplayPopupPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.message);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> Popup.put(Components.translateOrElseStr(this.message)));
        context.get().setPacketHandled(true);
    }
}
