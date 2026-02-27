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
        FHNetwork.INSTANCE.sendPlayer(player, new DisplayTipPacket(id));
    }

    /**
     * 使客户端显示对应 id 的 tip
     * @param id tip 的 id，在文件中定义
     */
    public static void sendGeneralToALl(String id) {
        FHNetwork.INSTANCE.sendToAll(new DisplayTipPacket(id));
    }

    /**
     * 向所有玩家发送自定义 tip
     * <p>
     * 注意：自定义 tip 不会储存任何状态
     * @param tip tip
     */
    public static void sendCustomToAll(Tip tip) {
        FHNetwork.INSTANCE.sendToAll(new DisplayCustomTipPacket(tip));
    }

    /**
     * 向玩家发送自定义 tip
     * <p>
     * 注意：自定义 tip 不会储存任何状态
     * @param tip tip
     * @param player 目标玩家
     */
    public static void sendCustom(Tip tip, ServerPlayer player) {
        FHNetwork.INSTANCE.sendPlayer(player, new DisplayCustomTipPacket(tip));
    }

    /**
     * 向玩家发送 popup
     * @param message 消息
     * @param player 目标玩家
     */
    public static void sendPopup(String message, ServerPlayer player) {
        FHNetwork.INSTANCE.sendPlayer(player, new DisplayPopupPacket(message));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(String message) {
        FHNetwork.INSTANCE.sendToAll(new DisplayPopupPacket(message));
    }

    /**
     * 向玩家发送 popup
     * @param message 消息
     * @param player 目标玩家
     */
    public static void sendPopup(Component message, ServerPlayer player) {
        FHNetwork.INSTANCE.sendPlayer(player, new DisplayPopupPacket(message));
    }

    /**
     * 向所有玩家发送 popup
     * @param message 消息
     */
    public static void sendPopupToAll(Component message) {
        FHNetwork.INSTANCE.sendToAll(new DisplayPopupPacket(message));
    }
}
