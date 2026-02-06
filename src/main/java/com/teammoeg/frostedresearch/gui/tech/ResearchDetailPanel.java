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

import java.util.List;

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.TechScrollBar;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ResearchDetailPanel extends UILayer {
	public TechScrollBar scrollInfo;
	public TechScrollBar scrolldetail;
	Research research;
	CIcon icon;
	ResearchDashboardPanel dashboardPanel;
	ResearchInfoPanel infoPanel;
	DescPanel descPanel;
	ResearchLayer researchScreen;

	public ResearchDetailPanel(ResearchLayer panel) {
		super(panel);
		descPanel = new DescPanel(this);
		infoPanel = new ResearchInfoPanel(this);
		scrollInfo = new TechScrollBar(this, infoPanel);
		scrolldetail = new TechScrollBar(this, descPanel);
		dashboardPanel = new ResearchDashboardPanel(this);
		researchScreen = panel;
		this.setZIndex(600);
	}

	@Override
	public void getTooltip(TooltipBuilder list) {
		list.translateZ(300);
		super.getTooltip(list);
	}

	@Override
	public void addUIElements() {
		if (research == null)
			return;
		icon = research.getIcon();

		add(dashboardPanel);
		dashboardPanel.setPosAndSize(4, 8, 140, 56);

		add(descPanel);
		descPanel.setPosAndSize(8, 66, 132, 98);
		add(scrolldetail);
		scrolldetail.setPosAndSize(142, 64, 8, 100);

		add(infoPanel);
		infoPanel.setPosAndSize(150, 15, 135, 151);
		Button closePanel = new Button(this) {
			@Override
			public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
			}

			@Override
			public void onClicked(MouseButton mouseButton) {
				close();
			}
		};
		closePanel.setPosAndSize(284, 7, 9, 8);
		add(closePanel);

		scrollInfo.setPosAndSize(285, 18, 8, 146);
		// scrollInfo.setMaxValue(height);
		add(scrollInfo);
		// already committed items
		// ResearchData rd = research.getData();
		TextField status = new TextField(this);
		status.setMaxWidth(135);
		/*
		 * if (research.getData().isInProgress()) {
		 * status.setText(GuiUtils.translateGui("research.in_progress").mergeStyle(
		 * TextFormatting.BOLD) .mergeStyle(TextFormatting.BLUE)); } else if
		 * (rd.canResearch()) {
		 * status.setText(GuiUtils.translateGui("research.can_research").mergeStyle(
		 * TextFormatting.BOLD) .mergeStyle(TextFormatting.GREEN));
		 * 
		 * }
		 */
		status.setPos(0, 6);
		add(status);

	}

	@Override
	public void alignWidgets() {
	}

	public void close() {
		this.research = null;
		// this.refresh();
		researchScreen.closeModal(this);
		// researchScreen.refreshWidgets();
	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		super.render(matrixStack, x, y, w, h);
	}

	@Override
	public boolean isVisible() {
		return research != null;
	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		// drawBackground(matrixStack, theme, x, y, w, h);
		DrawDeskTheme.drawDialog(matrixStack, x, y, w, h);
	}

	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this) && research != null;
	}

	public void open(Research r) {
		this.research = r;
		this.refresh();
		scrollInfo.setValue(0);
		researchScreen.setModal(this);
		// researchScreen.refreshWidgets();

	}

	public static class DescPanel extends UILayer {
		ResearchDetailPanel detailPanel;

		public DescPanel(ResearchDetailPanel panel) {
			super(panel);
			detailPanel = panel;
		}

		@Override
		public void addUIElements() {
			List<Component> itxs = detailPanel.research.getDesc();
			int offset = 0;
			for (Component itx : itxs) {
				TextField desc = new TextField(this);
				add(desc);
				desc.setMaxWidth(width);
				desc.setPosAndSize(0, offset, width, height);
				desc.setText(itx);
				desc.setColor(DrawDeskTheme.getTextColor());
				offset += desc.getHeight() + 2;
			}
			if (offset + 3 > height) {
				detailPanel.scrolldetail.unhide();
				// detailPanel.scrolldetail.setMaxValue(offset + 3);
			} else
				detailPanel.scrolldetail.hide();
			// this.setHeight(offset+3);

		}

		@Override
		public void alignWidgets() {

		}
	}
}
