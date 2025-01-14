package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.tips.ServerTipSender;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DisplayCustomTipRequestPacket(Tip tip) implements FHMessage {

    public DisplayCustomTipRequestPacket(FriendlyByteBuf buffer) {
        this(Tip.builder("").fromNBT(buffer.readNbt()).build());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        tip.write(buffer);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                FHMain.LOGGER.warn("{} IS A HACKER!", player.getName().getString());
                ServerTipSender.sendCustom(Tip.builder("warning").line(Lang.str("HACKER!")).pin(true).alwaysVisible(true).build(), player);
            } else {
                context.get().enqueueWork(() -> ServerTipSender.sendCustomToAll(tip));
            }
        }
        context.get().setPacketHandled(true);
    }
}
