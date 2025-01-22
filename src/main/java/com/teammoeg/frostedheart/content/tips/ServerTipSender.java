package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayPopupPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ServerTipSender {

    /**
     * 使客户端显示对应 id 的 tip
     * @param id tip 的 id，在文件中定义
     * @param player 目标玩家
     */
    public static void sendGeneral(String id, ServerPlayer player) {
        FHNetwork.sendPlayer(player, new DisplayTipPacket(id));
    }

    /**
     * 向所有玩家发送自定义 tip
     * <p>
     * 注意：自定义 tip 不会储存任何状态
     * @param tip tip
     */
    public static void sendCustomToAll(Tip tip) {
        FHNetwork.sendToAll(new DisplayCustomTipPacket(tip));
    }

    /**
     * 向玩家发送自定义 tip
     * <p>
     * 注意：自定义 tip 不会储存任何状态
     * @param tip tip
     * @param player 目标玩家
     */
    public static void sendCustom(Tip tip, ServerPlayer player) {
        FHNetwork.sendPlayer(player, new DisplayCustomTipPacket(tip));
    }

    /**
     * 向玩家发送 popup
     * @param message 消息
     * @param player 目标玩家
     */
    public static void sendPopup(String message, ServerPlayer player) {
        FHNetwork.sendPlayer(player, new DisplayPopupPacket(message));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(String message) {
        FHNetwork.sendToAll(new DisplayPopupPacket(message));
    }

    /**
     * 向玩家发送 popup
     * @param message 消息
     * @param player 目标玩家
     */
    public static void sendPopup(Component message, ServerPlayer player) {
        FHNetwork.sendPlayer(player, new DisplayPopupPacket(message));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(Component message) {
        FHNetwork.sendToAll(new DisplayPopupPacket(message));
    }
}
