package com.teammoeg.frostedheart.research;

import net.minecraft.util.text.ITextComponent;

/**
 * Interface for "Clue"
 * A contribution to research in percentage 
 * could achieve by player
 * */
public interface IClue {
	float getResearchContribution();
	String getID();
	boolean onClueEvent(ClueMessage cm);
	ITextComponent getName();
	ITextComponent getDescription();
	ITextComponent getHint();
	boolean isPendingAtStart();
}
