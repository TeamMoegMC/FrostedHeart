package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ClientResearchGame;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;

public class OrderWidget extends Widget {
	ClientResearchGame rg;
	int cardstate;
	public OrderWidget(Panel p, ClientResearchGame rg, int cardstate) {
		super(p);
		this.rg = rg;
		this.cardstate = cardstate;
	}
	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		
		DrawDeskIcons.ORDER_FRAME.draw(matrixStack, x, y,16,16);
		if(cardstate!=0) {
			CardStat cs=rg.getStats().get(cardstate);
			if(cs.card!=8)
				DrawDeskIcons.ORDER_ARROW.draw(matrixStack,x,y+16,16, 12);
			DrawDeskIcons.getIcon(cs.type,cs.card,true).draw(matrixStack, x, y, 16, 16);
			if(cs.num<=0) {
				TechIcons.FIN.draw(matrixStack, x, y, 16, 16);
			}
		}else
			TechIcons.DOTS.draw(matrixStack, x, y, 16,16);
	}


}
