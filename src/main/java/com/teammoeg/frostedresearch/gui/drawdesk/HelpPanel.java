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

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.contentpanel.LineHelper;
import com.teammoeg.chorda.client.ui.UV;
import com.teammoeg.frostedheart.content.archive.Alignment;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.TechScrollBar;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

class HelpPanel extends UILayer {
	DrawDeskLayer ot;
	ContentPanel contentPanel;

	public HelpPanel(DrawDeskLayer panel) {
		super(panel);
		ot = panel;
	}

	@Override
	public void addUIElements() {
		Button closePanel = new Button(this) {
			@Override
			public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
			}

			@Override
			public void onClicked(MouseButton mouseButton) {
				ot.closeHelp();
			}
		};
		closePanel.setPosAndSize(226, 7, 9, 8);
		add(closePanel);

		contentPanel = new ContentPanel(this) {
			@Override
			public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
			}

			@Override
			public void addUIElements() {
				super.addUIElements();
			}

			@Override
			public void resize() {
				scrollBar.setPosAndSize(getX() + getWidth()+10, 20, 7, getHeight()-15);
			}
		};
		contentPanel.fillContent(List.of(
				LineHelper.text (contentPanel, Lang.translateGui("minigame.match")).alignment(Alignment.CENTER).color(DrawDeskTheme.getTextColor()).scale(2),
				LineHelper.img  (contentPanel, DrawDeskIcons.LOCATION).uvOverride(new UV(243, 319, 73, 56, 512, 512)),
				LineHelper.text (contentPanel, Lang.translateGui("minigame.t1")).alignment(Alignment.CENTER).color(DrawDeskTheme.getTextColor()),
				LineHelper.br   (contentPanel).icon(TechIcons.HLINE).height(1),
				LineHelper.img  (contentPanel, DrawDeskIcons.LOCATION).uvOverride(new UV(316, 319, 90, 56, 512, 512)),
				LineHelper.text (contentPanel, Lang.translateGui("minigame.t2")).alignment(Alignment.CENTER).color(DrawDeskTheme.getTextColor()),
				LineHelper.space(contentPanel, 16),
				LineHelper.text (contentPanel, Lang.translateGui("minigame.display")).alignment(Alignment.CENTER).color(DrawDeskTheme.getTextColor()).scale(2),
				LineHelper.img  (contentPanel, DrawDeskIcons.LOCATION).uvOverride(new UV(243, 375, 93, 48, 512, 512)),
				LineHelper.text (contentPanel, Lang.translateGui("minigame.t3")).alignment(Alignment.CENTER).color(DrawDeskTheme.getTextColor()),
				LineHelper.br   (contentPanel).icon(TechIcons.HLINE).height(1),
				LineHelper.img  (contentPanel, DrawDeskIcons.LOCATION).uvOverride(new UV(336, 375, 74, 48, 512, 512)),
				LineHelper.text (contentPanel, Lang.translateGui("minigame.t4")).alignment(Alignment.CENTER).color(DrawDeskTheme.getTextColor()),
				LineHelper.br   (contentPanel).icon(TechIcons.HLINE).height(1),
				LineHelper.img  (contentPanel, DrawDeskIcons.LOCATION).uvOverride(new UV(243, 423, 79, 48, 512, 512)),
				LineHelper.text (contentPanel, Lang.translateGui("minigame.t5")).alignment(Alignment.CENTER).color(DrawDeskTheme.getTextColor())
		));
		contentPanel.scrollBar = new TechScrollBar(this, contentPanel);
		contentPanel.setPosAndSize(5, 5, 212, 160);
		add(contentPanel);
		add(contentPanel.scrollBar);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void render(GuiGraphics guiGraphics, int x, int y, int w, int h) {
		if (ot.showHelp) {
			super.render(guiGraphics, x, y, w, h);
		}
	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		DrawDeskIcons.HELP.draw(matrixStack, x, y, w, h);
	}

	@Override
	public boolean isEnabled() {
		return ot.showHelp;
	}

}