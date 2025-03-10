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

package com.teammoeg.frostedresearch.gui.drawdesk.game;

public class Card {
    CardType ct = CardType.NONE;
    int card;
    boolean show;
    //boolean sel;
    boolean unplacable;

    public void clear() {
        ct = CardType.NONE;
        card = -1;
        show = false;
        //sel=false;
        unplacable = false;
    }

    public int getCard() {
        return card;
    }

    public CardType getCt() {
        return ct;
    }

    public boolean isEmpty() {
        return !show;
    }

    public boolean isPlacable() {
        return !(unplacable || show);
    }

    public boolean isShow() {
        return show;
    }

    public boolean isUnplacable() {
        return unplacable;
    }

    public boolean match(Card other) {
        if (ct == other.ct) {
            return ct.match(card, other.card);
        }
        return false;
    }

    public int pack() {
        return card + (ct.ordinal() << 16);
    }

    public void read(int state) {
        show = (state & 0x01) > 0;
        //sel=(state&0x02)>0;
        unplacable = (state & 0x04) > 0;
        ct = CardType.values()[(state >> 4) & 0xf];
        card = (state >> 8) & 0xf;
    }

    public int serialize() {
        int state = 0;
        if (show)
            state |= 0x01;
		/*if(sel)
			state|=0x02;*/
        if (unplacable)
            state |= 0x04;
        state |= ct.ordinal() << 4;
        state |= card << 8;
        return state;
    }

    public void setType(CardType ct, int card) {
        this.ct = ct;
        this.card = card;
    }

    public void show() {
        show = true;
    }

    public String toString() {
        switch (ct) {
            case NONE:
                return "";
            case SIMPLE:
                return card == 0 ? "*" : String.valueOf(card - 1);
            case PAIR:
                return card == 0 ? "u" : "v";
            case ADDING:
                return card == 0 ? "+" : String.valueOf('a' + card - 1);
        }
        return "";
    }

}