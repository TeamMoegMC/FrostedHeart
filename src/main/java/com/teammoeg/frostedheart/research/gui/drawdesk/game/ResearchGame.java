/*
 * Copyright (c) 2022 TeamMoeg
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundNBT;

public class ResearchGame {
    Card[][] cards = new Card[9][9];
    int addcur;
    int addmax;
    int lvl = -1;
    boolean finished = false;
    Consumer<ResearchGame> listener;

    public ResearchGame() {
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                cards[i][j] = new Card();
            }
    }

    public void reset() {
        resetState();
        clear();
    }

    private void resetState() {
        addcur = 1;
        lvl = -1;
        finished = false;

    }

    public boolean tryCombine(CardPos c1, CardPos c2) {
        if (c2 == null) {
            if (addcur == addmax && isTouchable(c1)) {
                Card c = get(c1);
                if (c.ct == CardType.ADDING && c.card == 8) {
                    c.show = false;
                    doWinPending();
                    return true;
                }
            }
        } else {
            if (canCombine(c1, c2)) {
                Card cc1 = get(c1);
                Card cc2 = get(c2);
                if (cc1.ct == CardType.ADDING) {
                    addcur++;
                }
                cc1.show = false;
                cc2.show = false;
                doWinPending();
                return true;
            }
        }
        return false;
    }

    public void doWinPending() {
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                if (cards[i][j].show)
                    return;
            }
        finished = true;

    }

    public boolean canCombine(CardPos c1, CardPos c2) {
        return canCombine(c1.x, c1.y, c2.x, c2.y);
    }

    public boolean canCombine(int x1, int y1, int x2, int y2) {
        if (isTouchable(x1, y1) && isTouchable(x2, y2)) {
            Card c1 = get(x1, y1);
            Card c2 = get(x2, y2);
            return c1.match(c2);
        }
        return false;
    }

    public void clear() {
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                cards[i][j].clear();
    }

    public void init(GenerateInfo gen, Random rnd) {
        resetState();
        List<CardCombo> sol = generateSolution(gen, rnd);
        int attempt = 0;
        try {
            while (!placeSolution(sol, gen, rnd)) {
                attempt++;
                if (attempt >= 10) {// maybe it is impossible? change another solution.
                    sol = generateSolution(gen, rnd);
                    attempt = 0;
                }
            }
        } catch (Throwable t) {
            resetState();
        }
    }

    public boolean isTouchable(CardPos c1) {
        return isTouchable(c1.x, c1.y);
    }

    public boolean isTouchable(int x, int y) {
        boolean a, b, c, d, ac, ad, bc, bd;
        a = isEmpty(x + 1, y);
        b = isEmpty(x - 1, y);
        c = isEmpty(x, y + 1);
        d = isEmpty(x, y - 1);
        ac = isEmpty(x + 1, y + 1);
        ad = isEmpty(x + 1, y - 1);
        bc = isEmpty(x - 1, y + 1);
        bd = isEmpty(x - 1, y - 1);
        // return a+b+c+d>=2;
        Card cur = get(x, y);
        return (a && c && ac || a && d && ad || b && c && bc || b && d && bd) && !isEmpty(x, y)
                && (cur.ct != CardType.ADDING || cur.card == addcur || cur.card == 0
                || (addmax == addcur && cur.card == 8));
    }

    public Card get(int x, int y) {
        return cards[x][y];
    }

    public Card get(CardPos p) {
        return cards[p.x][p.y];
    }

    private void getPlacable(Set<CardPos> ret) {
        AddSector(ret, 4, 9, 3, -1);
        AddSector(ret, 3, -1, 4, -1);
        AddSector(ret, 4, -1, 5, 9);
        AddSector(ret, 5, 9, 4, 9);
    }

    private boolean isEmpty(int x, int y) {
        if (x < 0 || x >= 9 || y < 0 || y >= 9)
            return true;
        return cards[x][y].isEmpty();
    }

    private boolean isPlacable(int x, int y) {
        if (x < 0 || x >= 9 || y < 0 || y >= 9)
            return true;
        return cards[x][y].isPlacable();
    }

    private void AddSector(Set<CardPos> set, int x1, int x2, int y1, int y2) {
        int dx = x1 > x2 ? -1 : 1;
        int dy = y1 > y2 ? -1 : 1;
        for (int i = x1 + dx; i != x2; i += dx) {
            for (int j = y1 + dy; j != y2; j += dy) {
                if (isPlacable(i, j)) {
                    if (!isPlacable(i - dx, j) && !isPlacable(i, j - dy)) {
                        set.add(CardPos.valueOf(i, j));
                        break;
                    }
                }
            }
        }
        for (int i = x1; i != x2; i += dx) {
            if (isPlacable(i, y1)) {
                set.add(CardPos.valueOf(i, y1));
                break;
            }
        }
        for (int j = y1; j != y2; j += dy) {
            if (isPlacable(x1, j)) {
                set.add(CardPos.valueOf(x1, j));
                break;
            }
        }
    }

    private boolean placeSolution(List<CardCombo> sol, GenerateInfo gen, Random rnd) {
        clear();
        gen.setUnplacable(cards, rnd);
        Set<CardPos> place = new HashSet<>();
        cards[4][4].setType(CardType.ADDING, 8);
        cards[4][4].show = true;
        for (CardCombo cc : sol) {
            place.clear();
            getPlacable(place);
            // System.out.println(place.size());
            if (place.size() < 2) {
                return false;// generate attempt failed
            }
            int r1 = rnd.nextInt(place.size());
            int r2;
            do {
                r2 = rnd.nextInt(place.size());
            } while (r1 == r2);
            if (r1 > r2) {// swap
                int tmp = r1;
                r1 = r2;
                r2 = tmp;
            }
            Card c1 = null;
            Card c2 = null;
            int i = 0;
            for (CardPos cp : place) {
                if (i == r1) {
                    c1 = get(cp);
                }
                if (i == r2) {
                    c2 = get(cp);
                    break;
                } // cause r2 is always greater than r1
                i++;
            }
            // System.out.println(r1+","+r2+""+c1+""+c2);
            if (c1 == null || c2 == null)
                return false;// failed
            if (rnd.nextBoolean())
                cc.place(c1, c2);
            else
                cc.place(c2, c1);
        }
        return true;
    }

    private List<CardCombo> generateSolution(GenerateInfo gen, Random rnd) {
        List<CardCombo> solution = new ArrayList<>();
        // init parameters
        int numWild = gen.numWild;
        int numElms = gen.numElms;
        int numPairs = gen.numPairs;
        int numAdd = gen.numAdd;
        int totalSimple = numWild + numElms * 4;
        float WCchance = numWild * 1F / totalSimple;
        // Add simple pairs
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < numElms; j++)
                numWild -= pushSimple(solution, i + 1, numWild > 0, WCchance, rnd);
        // System.out.println(cs+":"+cs.pack());
        // add remaining wildcard
        if (numWild > 0)
            for (int i = 0; i < numWild; i++)
                solution.add(CardCombo.simple(0));
        // add pairs
        for (int j = 0; j < numPairs; j++)
            solution.add(CardCombo.pair(0));
        // System.out.println(cs+":"+cs.pack());
        solution.addAll(gen.getExtraCombo(rnd));
        // normal add fin
        Collections.shuffle(solution, rnd);
        // add add but preserver order
        addmax = numAdd + 1;
        addcur = 1;
        int[] inserts = new int[numAdd];
        for (int i = 0; i < numAdd; i++) {
            inserts[i] = rnd.nextInt(solution.size());
            /*
             * CardStat cs=new CardStat(CardType.ADDING,i);
             * cs.num=1;
             * stats.put(cs.pack(),cs);
             */
        }
        Arrays.sort(inserts);

        for (int i = 0; i < inserts.length; i++)
            solution.add(inserts[inserts.length - i - 1], CardCombo.add(i + 1));
        // solution generated
        return solution;
    }

    private int pushSimple(List<CardCombo> combos, int type, boolean shouldAdd, float chance, Random rnd) {
        if (shouldAdd && rnd.nextFloat() > chance) {
            combos.add(CardCombo.simpleW(type));
            combos.add(CardCombo.simpleW(type));
            return 1;
        }
        combos.add(CardCombo.simple(type));
        return 0;
    }

    public CompoundNBT serialize() {
        CompoundNBT cnbt = new CompoundNBT();
        cnbt.putInt("cur", addcur);
        cnbt.putInt("max", addmax);
        cnbt.putInt("lvl", lvl);
        int[] arr = new int[81];
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                arr[i + j * 9] = cards[i][j].serialize();
            }
        cnbt.putIntArray("cards", arr);
        return cnbt;
    }

    public void load(CompoundNBT data) {
        addcur = data.getInt("cur");
        addmax = data.getInt("max");

        lvl = data.getInt("lvl");
        int[] arr = data.getIntArray("cards");
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                cards[i][j].read(arr[i + j * 9]);
            }
        // this.calculateCardNum();
        this.onUpdate();
    }

    public void onUpdate() {
        if (listener != null)
            listener.accept(this);
    }

    public boolean isFinished() {
        return finished;
    }
    public boolean isEmpty() {
        return finished||lvl==-1;
    }
    public int getLvl() {
        return lvl;
    }

    public void setLvl(int lvl) {
        this.lvl = lvl;
    }

}
