package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ResearchGame;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;

public class CardStatPanel extends Panel {
	ResearchGame rg;
	int cardstate;
	TextField tf;


	public CardStatPanel(Panel panel, ResearchGame rg, int cardstate) {
		super(panel);
		this.rg = rg;
		this.cardstate = cardstate;
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		CardStat cs=rg.getStats().get(cardstate);
		tf.setText(""+cs.num);
		tf.setWidth(14);
		DrawDeskIcons.getIcon(cs.type, cs.card,true).draw(matrixStack,x, y-1,16,16);
		super.draw(matrixStack, theme, x, y, w, h);
	}

	@Override
	public void addWidgets() {
		tf=new TextField(this);
		tf.addFlags(4).setColor(TechIcons.text).setMaxWidth(15).setTrim();
		tf.setPosAndSize(1,16,14,8);
		add(tf);
	}

	@Override
	public void alignWidgets() {
	}

}
