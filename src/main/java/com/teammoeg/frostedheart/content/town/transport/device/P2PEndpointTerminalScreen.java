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
