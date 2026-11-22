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

package com.teammoeg.frostedheart.content.stratadrill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server game-mode access for current drill work and the native wheel callbacks. */
public interface StrataDrillWorkAccess {
    /** Sends the original start tick if the player is still operating the drill. */
    void fh$sendStrataDrillSnapshot(ServerPlayer observer);

    /** Matches only this extra cell's original live tool or its own pre-wear vanilla loot snapshot. */
    boolean fh$isDiscountedWheelStack(ItemStack stack);
}
