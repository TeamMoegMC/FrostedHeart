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

package com.teammoeg.frostedheart.content.town.client;

/**
 * 客户端的详细模式开关，由服务器的 {@code TownUiModeS2CPacket} 设置；界面每帧读取，变化时自行重建。
 * The client's copy of the detailed-mode switch, set by the server. Screens read it every frame and rebuild when
 * {@link #serial()} changes.
 */
public final class TownUiModeClient {
    private static boolean detailed;
    private static int serial;

    private TownUiModeClient() {
    }

    public static boolean detailed() {
        return detailed;
    }

    /** Changes whenever the switch does, so an open screen can notice. */
    public static int serial() {
        return serial;
    }

    public static void set(boolean value) {
        if (detailed != value) serial++;
        detailed = value;
    }
}
