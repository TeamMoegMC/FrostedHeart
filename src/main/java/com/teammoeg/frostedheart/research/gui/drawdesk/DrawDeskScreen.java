package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.gui.TechButton;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ResearchGame;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class DrawDeskScreen extends BaseScreen{
	ResearchGame rg=new ResearchGame();
	public DrawDeskScreen() {
	}

	@Override
	public void addWidgets() {
		for(int i=0;i<9;i++)
			for(int j=0;j<9;j++) {
				CardButton cb=new CardButton(this,rg,i,j);
				cb.setPosAndSize(165+17*i,28+17*j,17,17);
				add(cb);
			}
		int cnt=0;
		for(CardStat cs:rg.getStats().values()) {
			CardStatPanel csp=new CardStatPanel(this,rg,cs.pack());
			System.out.println(cs.toString());
			csp.setPosAndSize(319+(cnt%2)*15,26+(cnt/2)*28,16,28);
			add(csp);
			cnt++;
		}
		TechButton reset=new TechButton(this,DrawDeskIcons.RESET) {

			@Override
			public void onClicked(MouseButton arg0) {
				rg.init();
				refreshWidgets();
			}
			
		};
		reset.setPosAndSize(322,161,27,16);
		add(reset);
		
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		DrawDeskIcons.Background.draw(matrixStack, x, y, w, h);
	}

	@Override
	public boolean onInit() {
    	int sw=387;
    	int sh=203;
    	this.setSize(sw,sh);
		return true;
	}

}
