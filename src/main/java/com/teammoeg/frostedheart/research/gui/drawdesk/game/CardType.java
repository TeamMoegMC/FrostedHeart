package com.teammoeg.frostedheart.research.gui.drawdesk.game;

import java.util.function.BiFunction;

public enum CardType{
	NONE((a,b)->false,false),
	SIMPLE((a,b)->a==b||a==0||b==0,true),
	ADDING((a,b)->a==0||b==0,false),
	PAIR((a,b)->a/2==b/2&&a!=b,false);
	final BiFunction<Integer,Integer,Boolean> matcher;
	final boolean mustInPair;

	private CardType(BiFunction<Integer, Integer, Boolean> matcher, boolean mustInPair) {
		this.matcher = matcher;
		this.mustInPair = mustInPair;
	}

	public boolean match(int othis,int othat) {
		return matcher.apply(othis, othat);
	}
	public boolean isGood(int num) {
		return !mustInPair||num%2==0;
	}
}