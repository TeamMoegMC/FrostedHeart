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

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.LayerScrollBar;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.client.gui.GuiGraphics;

public class ResearchListPanel extends UILayer {

	public static final int RESEARCH_HEIGHT = 18;
	public static final int RES_PANEL_WIDTH = 80;
	public ResearchLayer researchScreen;
	public LayerScrollBar scroll;
	public ResearchList rl;

	public ResearchListPanel(ResearchLayer panel) {
		super(panel);
		researchScreen = panel;
	}

	@Override
	public void addUIElements() {
		rl = new ResearchList(this);
		scroll = new LayerScrollBar(this, rl);
		add(rl);
		add(scroll);
		scroll.setX(106);
		scroll.setSize(8, height);

	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		// theme.drawPanelBackground(matrixStack, x, y, w, h);
	}

	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this);
	}

	public static class ResearchButton extends Button {

		Research research;
		ResearchList listPanel;
		TextField tf;

		long lastupdate;

		public ResearchButton(ResearchList panel, Research research) {
			super(panel, research.getName(), research.getIcon());
			this.research = research;
			this.listPanel = panel;
			setSize(101, RESEARCH_HEIGHT);
			tf = new TextField(panel).setMaxLines(1).setMaxWidth(86).setText(research.getName());
			if (research.hasUnclaimedReward())
				tf.setColor(0x5555ff);
			else if (research.isCompleted()) {
				tf.setColor(0x229000);
			} else if (!research.isUnlocked()) {
				tf.setColor(getTheme().getErrorColor());
			}
			lastupdate = System.currentTimeMillis() / 1000;
		}

		@Override
		public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
			// CGuis.setupDrawing();
			this.drawIcon(matrixStack, x + 1, y + 1, 16, 16);
			long secs = System.currentTimeMillis() / 1000;
			if (lastupdate != secs) {
				lastupdate = secs;
				if (research.hasUnclaimedReward()) {
					if (secs % 2 == 0) {
						tf.setText(Lang.translateGui("research.unclaimed"));
					} else
						tf.setText(research.getName());
					tf.setColor(0x5555ff);
				} else if (research.isCompleted()) {
					tf.setColor(0x229000);
				} else if (!research.isUnlocked()) {
					tf.setColor(getTheme().getErrorColor());
				}
			}
			tf.render(matrixStack, x + 18, y + 6, 81, tf.getHeight());
			if (listPanel.researchScreen.selectedResearch == this.research)
				TechIcons.SELECTED.draw(matrixStack, x - 4, y + 7, 4, 4);
			DrawDeskTheme.INSTANCE.horizontalSplit(matrixStack, x, y+17, 99);
		}

		@Override
		public void onClicked(MouseButton mouseButton) {

			listPanel.researchScreen.selectResearch(research);
		}
	}

	public static class ResearchList extends UILayer {
		public ResearchLayer researchScreen;

		public ResearchList(ResearchListPanel panel) {
			super(panel);
			researchScreen = panel.researchScreen;
			this.setWidth(103);
			this.setHeight(118);
		}

		@Override
		public void addUIElements() {
			int offset = 0;

			for (Research r : FHResearch.getResearchesForRender(this.researchScreen.selectedCategory, FHResearch.editor)) {
				ResearchButton button = new ResearchButton(this, r);
				add(button);
				button.setPos(4, offset);
				offset += 18;
			}
			// this.setHeight(offset+1);
			// researchScreen.researchListPanel.scroll.setMaxValue(offset + 1);
		}

		@Override
		public void alignWidgets() {
		}

	}
}
