package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.teammoeg.frostedheart.research.gui.TechButton;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardType;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ClientResearchGame;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

class MainGamePanel extends Panel {
	ClientResearchGame rg;
	DrawDeskPanel ot;
	CardButton[][] cbs = new CardButton[9][9];

	public MainGamePanel(DrawDeskPanel panel, DrawDeskScreen p) {
		super(panel);
		ot = panel;
		rg = new ClientResearchGame(p.getTile().getGame(), p.getTile().getPos());
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < 9; j++) {
				CardButton cb = new CardButton(this, rg, i, j);
				cb.setPosAndSize(17 * i, 17 * j + 3, 17, 17);
				cbs[i][j] = cb;
			}
	}

	boolean enabled = true;

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void addWidgets() {
		rg.attach();
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < 9; j++) {
				add(cbs[i][j]);
			}
		int cntcs = 0;

		Panel states = new Panel(this) {

			@Override
			public void addWidgets() {
				int cntad = 0;
				int cntall = 0;
				for (CardStat cs : rg.getStats().values())
					if (cs.type == CardType.ADDING)
						cntall++;
				if (cntall <= 6) {
					for (CardStat cs : rg.getStats().values()) {
						if (cs.type == CardType.ADDING) {
							OrderWidget ow = new OrderWidget(this, rg, cs.pack());
							ow.setPosAndSize(0, 28 * cntad, 16, 28);
							add(ow);
							cntad++;
						}
					}
				}else {
					int cntig=0;
					for (CardStat cs : rg.getStats().values()) {
						if (cs.type == CardType.ADDING) {
							if(cntig<cntall-6&&cs.num==0) {
								cntig++;
								continue;
							}
							if(cntad>=5&&cs.card!=8) {
								OrderWidget ow2 = new OrderWidget(this, rg,0);
								ow2.setPosAndSize(0, 28 * cntad, 16, 28);
								add(ow2);
								break;
							}
							OrderWidget ow = new OrderWidget(this, rg, cs.pack());
							ow.setPosAndSize(0, 28 * cntad, 16, 28);
							add(ow);
							cntad++;
							
						}
					}
				}
			}

			@Override
			public void alignWidgets() {
			}
		};
		states.setPosAndSize(188, 0, 16, 164);
		add(states);
		for (CardStat cs : rg.getStats().values()) {
			if (cs.type != CardType.ADDING) {
				CardStatPanel csp = new CardStatPanel(this, rg, cs.pack());
				// System.out.println(cs.toString());
				csp.setPosAndSize(154 + (cntcs % 2) * 15, 1 + (cntcs / 2) * 28, 16, 28);
				add(csp);
				cntcs++;
			}
		}

		TechButton help = new TechButton(this, TechIcons.Question) {

			@Override
			public void onClicked(MouseButton arg0) {
				ot.openHelp();
			}
		};
		help.setPosAndSize(157, 116, 27, 16);
		add(help);
		TechButton reset = new TechButton(this, DrawDeskIcons.RESET) {

			@Override
			public void onClicked(MouseButton arg0) {
				rg.init();
				refreshWidgets();
			}
		};
		reset.setPosAndSize(157, 136, 27, 16);
		add(reset);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void onClosed() {
		rg.deinit();
	}
}