package com.teammoeg.frostedheart.research.gui.drawdesk.game;

class CardCombo{
	CardType ct;
	int c1;
	int c2;
	CardCombo(CardType ct, int c1, int c2) {
		super();
		this.ct = ct;
		this.c1 = c1;
		this.c2 = c2;
	}
	void place(Card c1,Card c2) {
		c1.setType(ct, this.c1);
		c1.show();
		c2.setType(ct, this.c2);
		c2.show();
	}
	static CardCombo simple(int t) {
		return new CardCombo(CardType.SIMPLE,t,t);
	}
	static CardCombo simpleW(int t) {
		return new CardCombo(CardType.SIMPLE,0,t);
	}
	
	static CardCombo add(int t) {
		return new CardCombo(CardType.ADDING,0,t);
	}
	static CardCombo pair(int t) {
		return new CardCombo(CardType.PAIR,t*2,t*2+1);
	}
}