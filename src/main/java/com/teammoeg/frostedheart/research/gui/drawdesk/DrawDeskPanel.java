package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.network.research.FHResearchControlPacket;
import com.teammoeg.frostedheart.network.research.FHResearchControlPacket.Operator;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.gui.TechButton;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardType;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ResearchGame;
import com.teammoeg.frostedheart.research.gui.tech.ResearchProgressPanel;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class DrawDeskPanel extends Panel {
	ResearchGame rg = new ResearchGame();
	DrawDeskScreen dd;
	public DrawDeskPanel(DrawDeskScreen p) {
		super(p);
		dd=p;
	}

	@Override
	public void addWidgets() {

		
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < 9; j++) {
				CardButton cb = new CardButton(this, rg, i, j);
				cb.setPosAndSize(165 + 17 * i, 28 + 17 * j, 17, 17);
				add(cb);
			}
		int cntcs = 0;
		int cntad = 0;
		for (CardStat cs : rg.getStats().values()) {
			if (cs.type == CardType.ADDING) {
				OrderWidget ow = new OrderWidget(this, rg, cs.pack());
				ow.setPosAndSize(353, 25 + 28 * cntad, 16, 28);
				add(ow);
				cntad++;
			} else {
				CardStatPanel csp = new CardStatPanel(this, rg, cs.pack());
				// System.out.println(cs.toString());
				csp.setPosAndSize(319 + (cntcs % 2) * 15, 26 + (cntcs / 2) * 28, 16, 28);
				add(csp);
				cntcs++;
			}
		}

		TechButton help = new TechButton(this, TechIcons.Question) {

			@Override
			public void onClicked(MouseButton arg0) {

			}
		};
		help.setPosAndSize(322, 141, 27, 16);
		add(help);
		TechButton reset = new TechButton(this, DrawDeskIcons.RESET) {

			@Override
			public void onClicked(MouseButton arg0) {
				rg.init();
				refreshWidgets();
			}
		};
		reset.setPosAndSize(322, 161, 27, 16);
		add(reset);
		ResearchProgressPanel p = new ResearchProgressPanel(this);
		p.setPosAndSize(14, 19, 111, 68);
		add(p);

		TechButton techTree = new TechButton(this, DrawDeskIcons.TECH) {

			@Override
			public void onClicked(MouseButton arg0) {
				dd.showTechTree();
			}


		};
		techTree.setPosAndSize(16, 68, 36, 19);

		add(techTree);
		TechButton techStop = new TechButton(this, DrawDeskIcons.STOP) {

			@Override
			public void onClicked(MouseButton arg0) {
				Research current=ClientResearchDataAPI.getData().getCurrentResearch().orElse(null);
				if(current!=null)
				PacketHandler.sendToServer(new FHResearchControlPacket(Operator.PAUSE,current));
			}
		};
		techStop.setPosAndSize(55, 68, 19, 19);
		add(techStop);
		int sw = 387;
		int sh = 203;
		this.setSize(sw, sh);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		DrawDeskIcons.Background.draw(matrixStack, x, y, w, h);
	}

	@Override
	public void alignWidgets() {
	}
	boolean enabled;

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
