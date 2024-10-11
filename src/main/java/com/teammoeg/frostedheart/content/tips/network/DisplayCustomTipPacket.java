package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DisplayCustomTipPacket implements FHMessage {
    private final String title;
    private final String content;
    private final int visibleTime;
    private final boolean history;

    public DisplayCustomTipPacket(FriendlyByteBuf buffer) {
        title = buffer.readUtf();
        content = buffer.readUtf();
        visibleTime = buffer.readInt();
        history = buffer.readBoolean();
    }

    public DisplayCustomTipPacket(String title, String content, int visibleTime, boolean history) {
        this.title = title;
        this.content = content;
        this.visibleTime = visibleTime;
        this.history = history;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.title);
        buffer.writeUtf(this.content);
        buffer.writeInt(this.visibleTime);
        buffer.writeBoolean(this.history);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> TipDisplayManager.displayCustomTip(title, content, visibleTime, history));
        ctx.get().setPacketHandled(true);
    }
}
