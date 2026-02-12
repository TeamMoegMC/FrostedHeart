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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.text.CFormatHelper;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.gui.ResearchEditUtils;
import com.teammoeg.frostedresearch.gui.TechIcons;
import net.minecraft.client.gui.GuiGraphics;

public class ResearchDashboardPanel extends UILayer {



	ResearchDetailPanel detailPanel;
	TextField techpoint;
	TextField availableInsightLevel;
	boolean isClueNotCompleted = false;

	public ResearchDashboardPanel(ResearchDetailPanel panel) {
		super(panel);
		detailPanel = panel;
		techpoint = new TextField(this).setMaxWidth(100).setMaxLines(1);
		techpoint.setPos(40, 20 + ClientUtils.font().lineHeight);
		availableInsightLevel = new TextField(this).setMaxWidth(100).setMaxLines(1);
		availableInsightLevel.setPos(40, 20 + ClientUtils.font().lineHeight * 2);
		availableInsightLevel.setMaxLines(1);
	}

	@Override
	public void addUIElements() {
		// close panel button
		/*
		 * Button closePanel = new SimpleTextButton(this, new
		 * StringTextComponent("Close"), Icon.EMPTY) {
		 * 
		 * @Override public void onClicked(MouseButton mouseButton) {
		 * detailPanel.close(); //closeGui(); } };
		 * closePanel.setPosAndSize(width-PADDING, 0, PADDING, PADDING);
		 * add(closePanel);
		 */
		TextField tf = new TextField(this);
		tf.setPos(0, 0);
		add(tf);
		tf.setMaxWidth(140).setMinWidth(140).setMaxLines(2).addFlags(4);
		tf.setText(detailPanel.research.getName());

		TextField tp = new TextField(this).setMaxWidth(140).setMaxLines(1);
		tp.setPos(40, 20);
		add(tp);
		tp.setText(Lang.translateGui("research.points"));
		tp.setX(140 - tp.getWidth());

		if (FHResearch.editor) {
			TextButton create = new TextButton(this, Components.str("edit"),
				CIcons.nop()) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					if (detailPanel.research != null)
						ResearchEditUtils.editResearch(this, detailPanel.research);
				}
			};
			create.setPos(40, 30);
			add(create);
		}
		ResearchData rd = detailPanel.research.getData();
		if (rd.canResearch()) {
			if (!rd.canComplete(detailPanel.research)) {
				tp.setColor(getTheme().getErrorColor());
				techpoint.setColor(getTheme().getErrorColor());
			}
			techpoint.setText(CFormatHelper.toReadableUnit(rd.getTotalCommitted(detailPanel.research)) + "/" + CFormatHelper.toReadableUnit(detailPanel.research.getRequiredPoints()) + "IOPS");
		}
		else
		techpoint.setText(CFormatHelper.toReadableUnit(detailPanel.research.getRequiredPoints()) + "IOPS");
		add(techpoint);
		
		
		techpoint.setX(140 - techpoint.getWidth());
		if (!rd.canResearch()) {
			int insightNeeded = detailPanel.research.getInsight();
			int insightAvailable = ClientResearchDataAPI.getData().get().getAvailableInsightLevel();
			availableInsightLevel.setText(Lang.translateGui("research.insight_required", insightNeeded, insightAvailable));
			if (insightNeeded > insightAvailable) {
				availableInsightLevel.setColor(getTheme().getErrorColor());
			}
			add(availableInsightLevel);
			availableInsightLevel.setX(140 - availableInsightLevel.getWidth());
		}
		if (rd.canResearch() && !rd.canComplete(detailPanel.research)) {
			TextField rq = new TextField(this).setMaxWidth(140).setMaxLines(1).setColor(getTheme().getErrorColor());
			rq.setPos(40, 20 + ClientUtils.font().lineHeight * 3);
			add(rq);
			rq.setText(Lang.translateGui("research.required_clue"));
			rq.setX(140 - rq.getWidth());
		}
	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {

		super.render(matrixStack, x, y, w, h);

		// name
		// theme.drawString(matrixStack, detailPanel.research.getName(), x+7, y+8);
		// icon
		TechIcons.SHADOW.draw(matrixStack, x + 1, y + 36, 36, 9);
		detailPanel.icon.draw(matrixStack, x + 3, y + 10, 32, 32);
		CGuiHelper.resetGuiDrawing();
		TechIcons.HLINE_L.draw(matrixStack, x, y + 55, 140, 3);

		// TODO: research progress
		// ResearchData data = ResearchDataAPI.getData((ServerPlayerEntity)
		// detailPanel.researchScreen.player).getData(detailPanel.research);
		// theme.drawString(matrixStack, data.getProgress()*100 + "%",
		// x+theme.getStringWidth(detailPanel.research.getName())+5, y);
	}
}
