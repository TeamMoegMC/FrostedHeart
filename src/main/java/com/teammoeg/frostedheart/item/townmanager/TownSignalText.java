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

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class TownSignalText {
    private TownSignalText() {
    }

    static Component describe(TownSignalEvent.Type type, int affectedCount) {
        return Component.translatable("gui.frostedheart.town_manager.event."
                + type.name().toLowerCase(Locale.ROOT), affectedCount);
    }

    static int color(TownSignalEvent.Severity severity) {
        return switch (severity) {
            case INFORMATION -> 0xFF55FFFF;
            case WARNING -> 0xFFFFAA00;
            case CRITICAL -> 0xFFFF5555;
            case IRREVERSIBLE -> 0xFFAA55FF;
        };
    }
}
