/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

/** Server-verified role of one physical P2P terminal. */
public enum P2PTerminalRole {
    SHIPPING(true, false),
    RECEIVING(false, true),
    BIDIRECTIONAL(true, true);

    public static final Codec<P2PTerminalRole> CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown P2P terminal role: " + name);
        }
    }, role -> role.name().toLowerCase(Locale.ROOT));

    private final boolean canSend;
    private final boolean canReceive;

    P2PTerminalRole(boolean canSend, boolean canReceive) {
        this.canSend = canSend;
        this.canReceive = canReceive;
    }

    public boolean canSend() {
        return canSend;
    }

    public boolean canReceive() {
        return canReceive;
    }
}
