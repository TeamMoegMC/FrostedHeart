package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.gui.TechButton;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardType;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ClientResearchGame;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.research.machines.DrawingDeskTileEntity;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.StringTextComponent;

class MainGamePanel extends Panel {
	ClientResearchGame rg;
	DrawDeskPanel ot;
	TechButton reset;
	TextField status;
	int lstatus=0;
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
		reset = new TechButton(this, DrawDeskIcons.RESET) {

			@Override
			public void onClicked(MouseButton arg0) {
				rg.init();
				refreshWidgets();
			}
		};
		reset.setPosAndSize(157, 136, 27, 16);
		status=new TextField(this).addFlags(Theme.CENTERED).addFlags(Theme.CENTERED_V).setMaxWidth(108).setColor(TechIcons.text);
	
		status.setPosAndSize(22, 54, 108, 50);
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
		
		add(reset);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void onClosed() {
		rg.deinit();
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		
		super.draw(matrixStack, theme, x, y, w, h);
		if(lstatus!=0) {
			DrawDeskIcons.DIALOG_FRAME.draw(matrixStack, x+7, y+54,137,52);
			status.draw(matrixStack, theme,status.getX(), status.getY(),status.width,status.height);
		}
		if(ResearchListeners.fetchGameLevel()==-1) {
			if(lstatus!=4) {
				status.setText(GuiUtils.translateGui("minigame.no_clue"));
				status.setPosAndSize(22, 54, 108, 50);
				lstatus=4;
			}
			return;
		}
		
			if((reset.isMouseOver()&&!EnergyCore.hasEnoughEnergy(ClientUtils.getPlayer(),DrawingDeskTileEntity.ENERGY_PER_PAPER))||(!EnergyCore.hasEnoughEnergy(ClientUtils.getPlayer(),DrawingDeskTileEntity.ENERGY_PER_COMBINE))) {
				
				if(lstatus!=1) {
				status.setText(GuiUtils.translateGui("minigame.tired_to_research"));
				status.setPosAndSize(22, 54, 108, 50);
				lstatus=1;
				}
				return;
			}
		DrawingDeskTileEntity tile=ot.dd.getTile();
		if(!tile.isInkSatisfied(reset.isMouseOver()?5:1)) {
			if(lstatus!=2) {
				status.setText(GuiUtils.translateGui("minigame.no_ink"));
				status.setPosAndSize(22, 54, 108, 50);
				lstatus=2;
			}
			return;
		}else if(reset.isMouseOver()&&!tile.isPaperSatisfied()) {
			if(lstatus!=3) {
				status.setText(GuiUtils.translateGui("minigame.no_paper"));
				status.setPosAndSize(22, 54, 108, 50);
				lstatus=3;
			}
			return;
		}
		if(lstatus!=0) {
			status.setText(StringTextComponent.EMPTY);
			lstatus=0;
		}
		
	}
}