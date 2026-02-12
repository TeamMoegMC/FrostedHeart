/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.gui.tech;

import com.ibm.icu.text.NumberFormat;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class CluePanel extends UILayer {
	public static final String sq = "☐";
	public static final String sq_v = "☑";
	public static final String sq_x = "☒";
	Clue c;
	Research r;
	Component hover;
	TextField clueName;
	TextField desc;
	TextField contribute;
	TextField rq;

	public CluePanel(UIElement panel, Clue c, Research r) {
		super(panel);
		this.c = c;
		this.r = r;
	}

	@Override
	public void getTooltip(TooltipBuilder list) {
		super.getTooltip(list);
		if (hover != null)
			list.accept(hover);
	}

	@Override
	public void addUIElements() {
		add(clueName);
		if (desc != null)
			add(desc);
		if (rq != null)
			add(rq);
		add(contribute);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		// super.drawBackground(matrixStack, theme, x, y, w, h);
		DrawDeskTheme.drawCheckBox(matrixStack, x, y, 9, 9, ClientResearchDataAPI.getData().get().isClueCompleted(r, c), r.isCompleted());
	}

	public void initWidgets() {
		int offset = 1;
		clueName = new TextField(this);
		clueName.setMaxWidth(width - 6).setText(c.getName(r)).setPos(10, offset);

		offset += clueName.getHeight() + 2;
		Component itx = c.getDescription(r);
		if (itx != null) {
			desc = new TextField(this);
			desc.setMaxWidth(width).setText(itx).setPos(0, offset);
			offset += desc.getHeight() + 2;
		}
		if (c.isRequired()) {
			rq = new TextField(this)
				.setMaxWidth(width)
				.setText(Lang.translateGui("research.required"))
				.setColor(getTheme().getErrorColor());
			rq.setPos(0, offset);
			offset += rq.getHeight() + 2;
		}
		contribute = new TextField(this)
			.setMaxWidth(width)
			.setText(Components.str("+" + NumberFormat.getPercentInstance().format(c.getResearchContribution())));
		contribute.setPos(0, offset);
		offset += contribute.getHeight() + 2;
		offset += 1;
		hover = c.getHint(r);
		this.setHeight(offset);
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (isEnabled()) {
				// TODO edit clue
			}

			return true;
		}

		return false;
	}

}
