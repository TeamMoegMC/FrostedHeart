/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
