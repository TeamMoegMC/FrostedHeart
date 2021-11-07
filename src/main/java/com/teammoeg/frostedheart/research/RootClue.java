package com.teammoeg.frostedheart.research;

import java.util.function.Predicate;

import net.minecraft.util.text.ITextComponent;

public class RootClue extends AbstractClue {
	Predicate<ClueMessage> ev;
	public RootClue(String ID, float contribution, ITextComponent name, ITextComponent desc, ITextComponent hint,Predicate<ClueMessage> onEvent,boolean pend) {
		super(ID, contribution, name, desc, hint,pend);
		ev=onEvent;
	}

	@Override
	public boolean onClueEvent(ClueMessage cm) {
		return ev.test(cm);
	}

}
