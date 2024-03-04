/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.gui.drawdesk.game;

public class CardStat {
    public final CardType type;
    public final int card;
    public int num;
    public int tot;

    public CardStat(CardType type, int card) {
        super();
        this.type = type;
        this.card = card;
        this.num = 0;
        this.tot = 0;
    }

    public boolean isGood() {
        return type.isGood(num);
    }

    public int pack() {
        return card + (type.ordinal() << 16);
    }

    @Override
    public String toString() {
        return "CardStat [type=" + type + ", card=" + card + ", num=" + num + "]";
    }
}
