/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dedicated screen for bidirectional terminals and their two buffer groups. */
public final class P2PBidirectionalTerminalScreen extends P2PTerminalScreen {
    public P2PBidirectionalTerminalScreen(
            P2PTerminalMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, true);
    }
}
