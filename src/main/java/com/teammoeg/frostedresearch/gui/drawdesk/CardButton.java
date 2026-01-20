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

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.frostedresearch.gui.drawdesk.game.Card;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedresearch.gui.drawdesk.game.ClientResearchGame;

import net.minecraft.client.gui.GuiGraphics;

public class CardButton extends Button {
	CardPos card;
	ClientResearchGame game;

	public CardButton(UIElement panel, ClientResearchGame game, int x, int y) {
		super(panel);
		this.game = game;
		card = CardPos.valueOf(x, y);
	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
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
	public Cursor getCursor() {
		if (game.isTouchable(card))
			return Cursor.HAND;
		return super.getCursor();
	}

	@Override
	public void onClicked(MouseButton arg0) {
		game.select(card);
	}

}
