package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.gui.TechIcons;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;

public class ResearchProgressPanel extends Panel {

    public ResearchProgressPanel(Panel panel) {
        super(panel);
    }

    @Override
    public void addWidgets() {

    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        // title
        theme.drawString(matrixStack, GuiUtils.translateGui("research_progress"), x+3, y,TechIcons.text,0);
        // progress bar
        // TODO: this cause crash when root clue is added
        // float progress = researchScreen.getInProgressResearch().getProgressFraction();
        // float reqTime = researchScreen.getInProgressResearch().getCurrentPoints();
        // float finTIme = researchScreen.getInProgressResearch().getRequiredPoints();
        Research inprog=ClientResearchDataAPI.getData().getCurrentResearch().orElse(null);
        if(inprog!=null) {
	        TechIcons.SLIDER_FRAME.draw(matrixStack,x+40, y+26,70,8);
	        TechIcons.drawTexturedRect(matrixStack,x+41, y+27,(int)(68f/100*35),6,true);
	        theme.drawString(matrixStack,"35%", x +90, y +34,TechIcons.text,0);
	        // research icon
	        TechIcons.SHADOW.draw(matrixStack, x+1, y+38, 36, 9);
	        
	        inprog.getIcon().draw(matrixStack, x+3, y+12,32,32);
	        theme.drawString(matrixStack, inprog.getName(), x + 40, y + 15,TechIcons.text,0);
	        TechIcons.HLINE_LR.draw(matrixStack, x+1, y+48,w-1,3);
        }else {
        	theme.drawString(matrixStack,GuiUtils.translateGui("no_active_research"), x + 40, y + 15,TechIcons.text,0);
        }
        // theme.drawString(matrixStack, researchScreen.getInProgressResearch().getDesc(), x + 50, y + 55, Color4I.GRAY, 0);

    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        //theme.drawPanelBackground(matrixStack, x, y, w, h);
    }
	/*@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this);
	}*/
}
