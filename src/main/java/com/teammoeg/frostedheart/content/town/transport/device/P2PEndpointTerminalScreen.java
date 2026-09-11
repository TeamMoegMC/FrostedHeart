/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact screen shared only by shipping and receiving endpoints. */
public final class P2PEndpointTerminalScreen extends P2PTerminalScreen {
    public P2PEndpointTerminalScreen(
            P2PTerminalMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, false);
    }
}
