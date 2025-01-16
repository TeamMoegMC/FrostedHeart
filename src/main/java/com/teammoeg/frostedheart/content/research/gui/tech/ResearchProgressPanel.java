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

package com.teammoeg.frostedheart.content.research.gui.tech;

import com.ibm.icu.text.NumberFormat;
import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.client.gui.GuiGraphics;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.gui.RTextField;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import com.teammoeg.frostedheart.content.research.research.Research;

import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;

public class ResearchProgressPanel extends Panel {

    public ResearchProgressPanel(Panel panel) {
        super(panel);
        this.getOnlyRenderWidgetsInside();
    }

    @Override
    public void addWidgets() {
        RTextField tf = new RTextField(this);
        tf.setMaxWidth(71).setMaxLine(2).setColor(TechIcons.text).setPos(40, 15);
        Research inprog = ClientResearchDataAPI.getData().get().getCurrentResearch().orElse(null);
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
    public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        // title

        theme.drawString(matrixStack, Lang.translateGui("research_progress"), x + 3, y, TechIcons.text, 0);
        // progress bar
        // TODO: this cause crash when root clue is added
        // float progress = researchScreen.getInProgressResearch().getProgressFraction();
        // float reqTime = researchScreen.getInProgressResearch().getCurrentPoints();
        // float finTIme = researchScreen.getInProgressResearch().getRequiredPoints();
        Research inprog = ClientResearchDataAPI.getData().get().getCurrentResearch().orElse(null);
        if (inprog != null) {
            float prog = inprog.getProgressFraction();
            TechIcons.SLIDER_FRAME.draw(matrixStack, x + 40, y + 32, 70, 8);
            if (prog > 0)
                TechIcons.drawTexturedRect(matrixStack, x + 41, y + 33, (int) (68f * prog), 6, true);
            if (inprog.getData().canComplete(inprog))
                theme.drawString(matrixStack, NumberFormat.getPercentInstance().format(prog), x + 90, y + 40, TechIcons.text, 0);
            else
                theme.drawString(matrixStack, Lang.translateGui("research.required_clue"), x + 40, y + 40, TechIcons.text_red, 0);
            // research icon

            TechIcons.SHADOW.draw(matrixStack, x + 1, y + 38, 36, 9);

            inprog.getIcon().draw(matrixStack, x + 3, y + 12, 32, 32);
            //theme.drawString(matrixStack, inprog.getName(), x + 40, y + 15,TechIcons.text,0);
            GuiHelper.setupDrawing();
            TechIcons.HLINE_LR.draw(matrixStack, x + 1, y + 48, w - 1, 3);
        }/*else {
        	theme.drawString(matrixStack,, x + 40, y + 15,TechIcons.text,0);
        }*/
        // theme.drawString(matrixStack, researchScreen.getInProgressResearch().getDesc(), x + 50, y + 55, Color4I.GRAY, 0);

    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        //theme.drawPanelBackground(matrixStack, x, y, w, h);
    }
	/*@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this);
	}*/


}
