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

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.research.network.FHDrawingDeskOperationPacket;

import net.minecraft.util.math.BlockPos;

public class ClientResearchGame implements Consumer<ResearchGame> {
    ResearchGame rg;
    CardPos lastSelect = null;//transient
    Map<Integer, CardStat> stats = new LinkedHashMap<>();//transient
    BlockPos bp;

    public ClientResearchGame(ResearchGame rg, BlockPos bp) {
        super();
        this.rg = rg;
        this.bp = bp;
        rg.listener = this;
        this.calculateCardNum();
    }

    public void reset() {
        lastSelect = null;
    }

    public void init() {
        FHPacketHandler.sendToServer(new FHDrawingDeskOperationPacket(bp));
    }

    public void deinit() {
        rg.listener = null;
    }

    public void attach() {
        rg.listener = this;
    }

    public boolean tryCombine(CardPos c1, CardPos c2) {
        if (c2 == null) {
            if (rg.addcur == rg.addmax && isTouchable(c1)) {
                Card c = get(c1);
                if (c.ct == CardType.ADDING && c.card == 8) {
                    FHPacketHandler.sendToServer(new FHDrawingDeskOperationPacket(bp, c1));
                    return true;
                }
            }
        } else {
            if (rg.canCombine(c1, c2)) {
                FHPacketHandler.sendToServer(new FHDrawingDeskOperationPacket(bp, c1, c2));
                return true;
            }
        }
        return false;
    }

    public void select(CardPos pos) {
        if (!rg.isTouchable(pos)) return;
        if (!pos.equals(lastSelect) && tryCombine(pos, lastSelect)) {
            lastSelect = null;
            return;
        }
        if (lastSelect == null) {
            lastSelect = pos;
        } else
            lastSelect = null;

    }

    public CardPos getLastSelect() {
        return lastSelect;
    }

    public void calculateCardNum() {
        stats.clear();
        Map<Integer, CardStat> stat = new HashMap<>();
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                Card c = get(i, j);
                CardStat cs = stat.computeIfAbsent(c.pack(), k -> new CardStat(c.ct, c.card));
                if (c.show) {
                    cs.num++;
                }
                cs.tot++;
            }
        stat.values().stream().filter(e -> e.tot > 0).filter(c -> (c.type == CardType.SIMPLE) || (c.type == CardType.PAIR && c.card % 2 == 0) || (c.type == CardType.ADDING && c.card != 0)
        ).sorted(Comparator.comparingInt(CardStat::pack)).forEach(e -> stats.put(e.pack(), e));

    }

    public Map<Integer, CardStat> getStats() {
        return stats;
    }

    public ResearchGame getGame() {
        return rg;
    }

    public boolean isTouchable(CardPos card) {
        return rg.isTouchable(card);
    }

    public Card get(CardPos card) {
        return rg.get(card);
    }

    public Card get(int x, int y) {
        return rg.get(x, y);
    }

    @Override
    public void accept(ResearchGame t) {
        this.calculateCardNum();
        ClientUtils.refreshResearchGui();
    }

    public int getLevel() {
        return rg.lvl;
    }
}
