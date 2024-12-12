package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class TipAPI {

    /**
     * 发送 tip
     */
    public static void sendGeneral(String id, ServerPlayer player) {
        FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new DisplayTipPacket(id));
    }

    /**
     * 发送自定义 tip
     */
    public static void sendCustom(Tip tip, ServerPlayer player) {
        FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new DisplayCustomTipPacket(tip));
    }
}
