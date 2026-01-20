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

package com.teammoeg.frostedresearch.gui.tech;

import com.ibm.icu.text.NumberFormat;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class ResearchProgressPanel extends UILayer {

	public ResearchProgressPanel(UIElement panel) {
		super(panel);
	}

	@Override
	public void addUIElements() {
		TextField tf = new TextField(this);
		tf.setMaxWidth(71).setMaxLines(2).setColor(TechIcons.text).setPos(40, 15);
		Research inprog = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
		if (inprog != null)
			tf.setText(inprog.getName());
		else
			tf.setText(Lang.translateGui("no_active_research"));
		add(tf);
	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		super.render(matrixStack, x, y, w, h);
		// title

		matrixStack.drawString(getFont(), Lang.translateGui("research_progress"), x + 3, y, TechIcons.text, false);
		// progress bar
		// TODO: this cause crash when root clue is added
		// float progress =
		// researchScreen.getInProgressResearch().getProgressFraction();
		// float reqTime = researchScreen.getInProgressResearch().getCurrentPoints();
		// float finTIme = researchScreen.getInProgressResearch().getRequiredPoints();
		Research inprog = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
		if (inprog != null) {
			float prog = inprog.getProgressFraction();
			TechIcons.SLIDER_FRAME.draw(matrixStack, x + 40, y + 32, 70, 8);
			int progressWidth = Mth.ceil(68f * prog);
			if (progressWidth > 1)
				TechIcons.drawTexturedRect(matrixStack, x + 41, y + 33, progressWidth, 6, true);
			if (inprog.getData().canComplete(inprog))
				matrixStack.drawString(getFont(), NumberFormat.getPercentInstance().format(prog), x + 90, y + 40, TechIcons.text, false);
			else
				matrixStack.drawString(getFont(), Lang.translateGui("research.required_clue"), x + 40, y + 40, TechIcons.text_red, false);
			// research icon

			TechIcons.SHADOW.draw(matrixStack, x + 1, y + 38, 36, 9);

			inprog.getIcon().draw(matrixStack, x + 3, y + 12, 32, 32);
			// theme.drawString(matrixStack, inprog.getName(), x + 40, y +
			// 15,TechIcons.text,0);
			CGuiHelper.resetGuiDrawing();
			TechIcons.HLINE_LR.draw(matrixStack, x + 1, y + 48, w - 1, 3);
		} /*
			 * else { theme.drawString(matrixStack,, x + 40, y + 15,TechIcons.text,0); }
			 */
		// theme.drawString(matrixStack,
		// researchScreen.getInProgressResearch().getDesc(), x + 50, y + 55,
		// Color4I.GRAY, 0);

	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
		// theme.drawPanelBackground(matrixStack, x, y, w, h);
	}
	/*
	 * @Override public boolean isEnabled() { return researchScreen.canEnable(this);
	 * }
	 */

}
