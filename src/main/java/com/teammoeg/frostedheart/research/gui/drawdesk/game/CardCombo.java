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

package com.teammoeg.frostedheart.research.gui.drawdesk.game;

class CardCombo {
    final CardType ct;
    final int c1;
    final int c2;

    CardCombo(CardType ct, int c1, int c2) {
        super();
        this.ct = ct;
        this.c1 = c1;
        this.c2 = c2;
    }

    void place(Card c1, Card c2) {
        c1.setType(ct, this.c1);
        c1.show();
        c2.setType(ct, this.c2);
        c2.show();
    }

    static CardCombo simple(int t) {
        return new CardCombo(CardType.SIMPLE, t, t);
    }

    static CardCombo simpleW(int t) {
        return new CardCombo(CardType.SIMPLE, 0, t);
    }

    static CardCombo add(int t) {
        return new CardCombo(CardType.ADDING, 0, t);
    }

    static CardCombo pair(int t) {
        return new CardCombo(CardType.PAIR, t * 2, t * 2 + 1);
    }
}