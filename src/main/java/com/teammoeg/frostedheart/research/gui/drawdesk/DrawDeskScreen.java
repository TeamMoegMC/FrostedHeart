package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.gui.TechButton;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardType;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ResearchGame;
import com.teammoeg.frostedheart.research.gui.tech.ResearchProgressPanel;
import com.teammoeg.frostedheart.research.gui.tech.ResearchPanel;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class DrawDeskScreen extends BaseScreen {
	ResearchGame rg = new ResearchGame();
	DrawDeskContainer cx;
	public DrawDeskScreen(DrawDeskContainer c) {
		cx=c;
	}

	@Override
	public void addWidgets() {
		ResearchPanel rs = new ResearchPanel(this) {

			@Override
			public void onDisabled() {
				this.setEnabled(false);
				cx.setEnabled(true);
			}
			
		};
		rs.setEnabled(false);
		rs.setPos(0, 0);
		
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < 9; j++) {
				CardButton cb = new CardButton(this, rg, i, j) {
					@Override
					public boolean isEnabled() {
						return !rs.isEnabled();
					}
				};
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

			@Override
			public boolean isEnabled() {
				return !rs.isEnabled();
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

			@Override
			public boolean isEnabled() {
				return !rs.isEnabled();
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
				rs.setEnabled(true);
				cx.setEnabled(false);
				
			}

			@Override
			public boolean isEnabled() {
				return !rs.isEnabled();
			}
		};
		techTree.setPosAndSize(16, 68, 36, 19);

		add(techTree);
		TechButton techClose = new TechButton(this, DrawDeskIcons.STOP) {

			@Override
			public void onClicked(MouseButton arg0) {
				// new ResearchScreen().openGui();

			}

			@Override
			public boolean isEnabled() {
				return !rs.isEnabled();
			}

		};
		techClose.setPosAndSize(55, 68, 19, 19);
		add(techClose);
		add(rs);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		DrawDeskIcons.Background.draw(matrixStack, x, y, w, h);
	}

	@Override
	public boolean onInit() {
		int sw = 387;
		int sh = 203;
		this.setSize(sw, sh);
		return true;
	}
}
