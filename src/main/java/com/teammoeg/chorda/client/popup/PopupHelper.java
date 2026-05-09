package com.teammoeg.chorda.client.popup;

import com.teammoeg.chorda.ChordaNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PopupHelper {

    /**
     * 向玩家发送 popup
     * @param message 消息
     * @param player 目标玩家
     */
    public static void sendPopup(String message, ServerPlayer player) {
        ChordaNetwork.INSTANCE.sendPlayer(player, new DisplayPopupPacket(message));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(String message) {
        ChordaNetwork.INSTANCE.sendToAll(new DisplayPopupPacket(message));
    }

    /**
     * 向玩家发送 popup
     * @param message 消息
     * @param player 目标玩家
     */
    public static void sendPopup(Component message, ServerPlayer player) {
        ChordaNetwork.INSTANCE.sendPlayer(player, new DisplayPopupPacket(message));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(Component message) {
        ChordaNetwork.INSTANCE.sendToAll(new DisplayPopupPacket(message));
    }
}
