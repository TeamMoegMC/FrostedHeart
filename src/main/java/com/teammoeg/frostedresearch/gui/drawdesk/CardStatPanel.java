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

import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedresearch.gui.drawdesk.game.ClientResearchGame;

import net.minecraft.client.gui.GuiGraphics;

public class CardStatPanel extends UILayer {
	ClientResearchGame rg;
	int cardstate;
	TextField tf;

	public CardStatPanel(UIElement panel, ClientResearchGame rg, int cardstate) {
		super(panel);
		this.rg = rg;
		this.cardstate = cardstate;
	}

	@Override
	public void addUIElements() {
		tf = new TextField(this);
		tf.addFlags(4).setColor(TechIcons.text).setMaxWidth(15).setTrim();
		tf.setWidth(15);
		tf.setPosAndSize(1, 16, 14, 8);
		add(tf);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		CardStat cs = rg.getStats().get(cardstate);
		tf.setColor(cs.isGood() ? TechIcons.text : TechIcons.text_red);
		tf.setText("" + cs.num);

		DrawDeskIcons.getIcon(cs.type, cs.card, true).draw(matrixStack, x, y - 1, 16, 16);
		super.render(matrixStack, x, y, w, h);
	}

}
