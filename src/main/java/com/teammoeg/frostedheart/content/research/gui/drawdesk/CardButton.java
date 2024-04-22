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

package com.teammoeg.frostedheart.content.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.Card;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.ClientResearchGame;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.CursorType;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class CardButton extends Button {
    CardPos card;
    ClientResearchGame game;

    public CardButton(Panel panel, ClientResearchGame game, int x, int y) {
        super(panel);
        this.game = game;
        card = CardPos.valueOf(x, y);
    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        Card c = game.get(card);
        if (c.isShow()) {
            if (game.isTouchable(card)) {
                DrawDeskIcons.getIcon(c.getCt(), c.getCard(), true).draw(matrixStack, x, y, 16, 16);
                if (super.isMouseOver() || (game.getLastSelect() != null && game.getLastSelect().equals(card)))
                    DrawDeskIcons.SELECTED.draw(matrixStack, x, y, 16, 16);
            } else {
                DrawDeskIcons.getIcon(c.getCt(), c.getCard(), false).draw(matrixStack, x, y, 16, 16);
            }
        }
    }

    @Override
    public CursorType getCursor() {
        if (game.isTouchable(card))
            return CursorType.HAND;
        return CursorType.ARROW;
    }

    @Override
    public void onClicked(MouseButton arg0) {
        game.select(card);
    }

}
