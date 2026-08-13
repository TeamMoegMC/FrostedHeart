/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
