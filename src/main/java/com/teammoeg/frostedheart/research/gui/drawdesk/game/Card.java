package com.teammoeg.frostedheart.research.gui.drawdesk.game;

public class Card{
	CardType ct=CardType.NONE;
	int card;
	boolean show;
	//boolean sel;
	boolean unplacable;
	int x;
	int y;
	public void clear() {
		ct=CardType.NONE;
		card=-1;
		show=false;
		//sel=false;
		unplacable=false;
	}
	public void setPos(int x,int y) {
		this.x=x;
		this.y=y;
	}
	public void setType(CardType ct,int card) {
		this.ct=ct;
		this.card=card;
	}
	public boolean match(Card other) {
		if(ct==other.ct) {
			return ct.match(card, other.card);
		}
		return false;
	}
	public boolean isEmpty() {
		return !show;
	}
	public boolean isPlacable() {
		return !(unplacable||show);
	}
	public void show() {
		show=true;
	}
	public int pack() {
		return card+(ct.ordinal()<<16);
	}
	public int serialize() {
		int state=0;
		if(show)
			state|=0x01;
		/*if(sel)
			state|=0x02;*/
		if(unplacable)
			state|=0x04;
		state|=ct.ordinal()<<4;
		state|=card<<8;
		state|=x<<12;
		state|=y<<16;
		return state;
	}
	public void read(int state) {
		show=(state&0x01)>0;
		//sel=(state&0x02)>0;
		unplacable=(state&0x04)>0;
		ct=CardType.values()[(state>>4)&0xf];
		card=(state>>8)&0xf;
		x=(state>>12)&0xf;
		y=(state>>16)&0xf;
	}
	public CardType getCt() {
		return ct;
	}
	public int getCard() {
		return card;
	}
	public boolean isShow() {
		return show;
	}
	public boolean isUnplacable() {
		return unplacable;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
}