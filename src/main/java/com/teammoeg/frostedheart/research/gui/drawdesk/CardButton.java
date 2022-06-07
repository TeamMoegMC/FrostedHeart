package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.Card;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ClientResearchGame;

import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.CursorType;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class CardButton extends Button {
	CardPos card;
	ClientResearchGame game;
	public CardButton(Panel panel,ClientResearchGame game,int x,int y) {
		super(panel);
		this.game=game;
		card=CardPos.valueOf(x, y);
	}

	@Override
	public void onClicked(MouseButton arg0) {
		game.select(card);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		Card c=game.get(card);
		if(c.isShow()) {
			if(game.isTouchable(card)) {
				DrawDeskIcons.getIcon(c.getCt(),c.getCard(),true).draw(matrixStack, x, y, 16,16);
				if(super.isMouseOver()||(game.getLastSelect()!=null&&game.getLastSelect().equals(card)))
					DrawDeskIcons.SELECTED.draw(matrixStack, x, y, 16, 16);
			}else {
				DrawDeskIcons.getIcon(c.getCt(),c.getCard(),false).draw(matrixStack, x, y, 16,16);
			}
		}
	}

	@Override
	public CursorType getCursor() {
		if(game.isTouchable(card))
			return CursorType.HAND;
		return CursorType.ARROW;
	}

}
