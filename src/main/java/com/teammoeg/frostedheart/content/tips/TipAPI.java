package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class TipAPI {

    /**
     * 使客户端显示对应 id 的 tip
     * @param id tip 的 id，在文件中定义
     * @param player 目标玩家
     */
    public static void sendGeneral(String id, ServerPlayer player) {
        FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new DisplayTipPacket(id));
    }

    /**
     * 向客户端发送自定义 tip
     * <p>
     * 注意：自定义 tip 不会储存任何状态
     * @param tip tip
     * @param player 目标玩家
     */
    public static void sendCustom(Tip tip, ServerPlayer player) {
        FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new DisplayCustomTipPacket(tip));
    }
}
