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

import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.function.BiConsumer;

@SuppressWarnings("unchecked")
public class GenerateInfo {

    public static final GenerateInfo T1 = new GenerateInfo(3, 2, 1, 4).generators((cards, rnd) -> {
        for (int j = 0; j < 9; j++) {
            cards[0][j].unplacable = true;
            cards[8][j].unplacable = true;
            cards[j][0].unplacable = true;
            cards[j][8].unplacable = true;
        }
        for (int i = 4 - 1; i <= 4 + 1; i++)
            for (int j = 4 - 1; j <= 4 + 1; j++)
                if (i != 4 || j != 4)
                    cards[i][j].unplacable = true;
        if (rnd.nextBoolean()) {
            cards[1][7].unplacable = true;
            cards[7][1].unplacable = true;
        } else {
            cards[7][7].unplacable = true;
            cards[1][1].unplacable = true;
        }
    }, (cards, rnd) -> {
        for (int j = 0; j < 9; j++) {
            cards[0][j].unplacable = true;
            cards[8][j].unplacable = true;
            cards[j][0].unplacable = true;
            cards[j][8].unplacable = true;
        }
        if (rnd.nextBoolean())
            for (int j = 2; j < 7; j++) {
                cards[1][j].unplacable = true;
                cards[7][j].unplacable = true;
            }
        else
            for (int j = 2; j < 7; j++) {
                cards[j][1].unplacable = true;
                cards[j][7].unplacable = true;
            }

    });// 39 +10E
    public static final GenerateInfo T2 = new GenerateInfo(4, 3, 2, 5).generators((cards, rnd) -> {
        for (int i = 0; i <= 1; i++) {
            cards[0][i].unplacable = true;
            cards[1][i].unplacable = true;
            cards[8][i].unplacable = true;
            cards[7][i].unplacable = true;
        }
        for (int i = 7; i <= 8; i++) {
            cards[0][i].unplacable = true;
            cards[1][i].unplacable = true;
            cards[8][i].unplacable = true;
            cards[7][i].unplacable = true;
        }
        for (int i = 4 - 1; i <= 4 + 1; i++)
            for (int j = 4 - 1; j <= 4 + 1; j++)
                if (i != 4 || j != 4)
                    cards[i][j].unplacable = true;
        cards[2][2].unplacable = true;
        cards[2][6].unplacable = true;
        cards[6][2].unplacable = true;
        cards[6][6].unplacable = true;
    }, (cards, rnd) -> {
        for (int i = 4 - 1; i <= 4 + 1; i++)
            for (int j = 4 - 1; j <= 4 + 1; j++)
                if (i != 4 || j != 4) {
                    cards[i][j].unplacable = true;
                }

        for (int i = 4 - 2; i <= 4 + 2; i++) {
            if (i != 4) {
                cards[i][1].unplacable = true;
                cards[i][7].unplacable = true;
                cards[1][i].unplacable = true;
                cards[7][i].unplacable = true;
            }
        }
        cards[0][0].unplacable = true;
        cards[0][8].unplacable = true;
        cards[8][0].unplacable = true;
        cards[8][8].unplacable = true;
    });// 53 +28E
    public static final GenerateInfo T3 = new GenerateInfo(5, 4, 2, 6).generators((cards, rnd) -> {
        for (int i = 2; i <= 6; i++)
            for (int j = 2; j <= 6; j++)
                if (i != 4 && j != 4)
                    cards[i][j].unplacable = true;
    }, (cards, rnd) -> {
        for (int i = 0; i <= 1; i++) {
            cards[0][i].unplacable = true;
            cards[1][i].unplacable = true;
            cards[9 - 1][i].unplacable = true;
            cards[9 - 2][i].unplacable = true;
        }
        for (int i = 9 - 2; i <= 9 - 1; i++) {
            cards[0][i].unplacable = true;
            cards[1][i].unplacable = true;
            cards[9 - 1][i].unplacable = true;
            cards[9 - 2][i].unplacable = true;
        }
    }, (cards, rnd) -> {
        for (int i = 4 - 1; i <= 4 + 1; i++) {
            if (i != 4) {
                cards[i][1].unplacable = true;
                cards[i][2].unplacable = true;
                cards[i][6].unplacable = true;
                cards[i][7].unplacable = true;
                cards[1][i].unplacable = true;
                cards[2][i].unplacable = true;
                cards[6][i].unplacable = true;
                cards[7][i].unplacable = true;
            }
        }
    });// 65 +16E
    public static final GenerateInfo T4 = new GenerateInfo(6, 5, 4, 7);// 81 +0E
    public static GenerateInfo[] all = new GenerateInfo[]{T1, T2, T3, T4};
    int numElms;
    int numPairs;
    int numWild;
    int numAdd;

    BiConsumer<Card[][], Random>[] generators;

    public GenerateInfo(int numElms, int numPairs, int numWild, int numAdd) {
        super();
        this.numElms = numElms;
        this.numPairs = numPairs;
        this.numWild = numWild;
        this.numAdd = numAdd;
    }

    public GenerateInfo generators(BiConsumer<Card[][], Random>... r) {
        generators = r;
        return this;
    }

    public Collection<CardCombo> getExtraCombo(Random rnd) {
        return Collections.EMPTY_LIST;

    }

    public void setUnplacable(Card[][] cards, Random rnd) {
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                cards[i][j].unplacable = false;
        if (generators != null)
            generators[rnd.nextInt(generators.length)].accept(cards, rnd);
    }
}
