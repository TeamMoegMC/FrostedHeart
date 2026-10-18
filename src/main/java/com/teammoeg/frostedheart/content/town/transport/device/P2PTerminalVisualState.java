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

package com.teammoeg.frostedheart.content.town.transport.device;

import net.minecraft.util.StringRepresentable;

/** Finite world-visible state; detailed transport facts remain in the menu snapshot. */
public enum P2PTerminalVisualState implements StringRepresentable {
    UNBOUND("unbound"),
    IDLE("idle"),
    TRANSFERRING("transferring"),
    REDSTONE_PAUSED("redstone_paused"),
    SHORTAGE("shortage"),
    RECEIVER_CONTAINER_UNAVAILABLE("receiver_container_unavailable"),
    PEER_UNLOADED("peer_unloaded"),
    UNAVAILABLE("unavailable");

    private final String serializedName;

    P2PTerminalVisualState(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
