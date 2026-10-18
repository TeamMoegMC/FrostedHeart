/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
        ChordaNetwork.INSTANCE.sendPlayer(player, new DisplayPopupPacket(Component.literal(message)));
    }

    /**
     * 向玩家发送 popup
     * @param message 消息
     * @param displayTime 显示时间
     * @param player 目标玩家
     */
    public static void sendPopup(String message, int displayTime, ServerPlayer player) {
        ChordaNetwork.INSTANCE.sendPlayer(player, new DisplayPopupPacket(Component.literal(message), displayTime));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(String message) {
        ChordaNetwork.INSTANCE.sendToAll(new DisplayPopupPacket(Component.literal(message)));
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
     * 向玩家发送 popup
     * @param message 消息
     * @param displayTime 显示时间
     * @param player 目标玩家
     */
    public static void sendPopup(Component message, int displayTime, ServerPlayer player) {
        ChordaNetwork.INSTANCE.sendPlayer(player, new DisplayPopupPacket(message, displayTime));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(Component message) {
        ChordaNetwork.INSTANCE.sendToAll(new DisplayPopupPacket(message));
    }
}
