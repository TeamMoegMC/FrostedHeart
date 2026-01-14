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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedresearch.gui.drawdesk.game.ClientResearchGame;

import net.minecraft.client.gui.GuiGraphics;

public class OrderWidget extends UIElement {
    ClientResearchGame rg;
    int cardstate;

    public OrderWidget(UIElement p, ClientResearchGame rg, int cardstate) {
        super(p);
        this.rg = rg;
        this.cardstate = cardstate;
    }

    @Override
    public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {

        DrawDeskIcons.ORDER_FRAME.draw(matrixStack, x, y, 16, 16);
        if (cardstate != 0) {
            CardStat cs = rg.getStats().get(cardstate);
            if (cs.card != 8)
                DrawDeskIcons.ORDER_ARROW.draw(matrixStack, x, y + 16, 16, 12);
            DrawDeskIcons.getIcon(cs.type, cs.card, true).draw(matrixStack, x, y, 16, 16);
            if (cs.num <= 0) {
                TechIcons.FIN.draw(matrixStack, x, y, 16, 16);
            }
        } else
            TechIcons.DOTS.draw(matrixStack, x, y, 16, 16);
    }


}
