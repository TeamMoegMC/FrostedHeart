/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.trade;

import com.teammoeg.frostedheart.util.client.GuiUtils;

import net.minecraft.util.text.TranslationTextComponent;

public enum RelationModifier {
    KILLED_HISTORY("history_killed"),
    UNKNOWN_LANGUAGE("unknown_language"),
    CHARM("charm"),
    KILLED_SAW("saw_murder"),
    HURT("hurt"),
    RECENT_BENEFIT("beneficial_trade"),
    TRADE_LEVEL("total_trade"),
    SAVED_VILLAGE("hero_village"),
    RECENT_BARGAIN("recent_bargain"),
    FOREIGNER("foreigner");
    public final String tkey;

    private RelationModifier(String tkey) {
        this.tkey = tkey;
    }

    public TranslationTextComponent getDesc() {
        return GuiUtils.translateGui("trade.relation." + tkey);
    }
}
