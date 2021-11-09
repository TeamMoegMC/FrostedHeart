package com.teammoeg.frostedheart.research;

import net.minecraft.util.text.ITextComponent;

public abstract class AbstractClue extends FHRegisteredItem implements IClue{
	float contribution;
	String ID;
	ITextComponent name;
	ITextComponent desc;
	ITextComponent hint;
	boolean pend;
	@Override
	public float getResearchContribution() {
		return contribution;
	}

	public AbstractClue(String ID,float contribution, ITextComponent name, ITextComponent desc, ITextComponent hint,boolean isPend) {
		this.contribution = contribution;
		this.ID = ID;
		this.name = name;
		this.desc = desc;
		this.hint = hint;
		this.pend=isPend;
	}

	@Override
	public boolean isPendingAtStart() {
		return pend;
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public ITextComponent getName() {
		return name;
	}

	@Override
	public ITextComponent getDescription() {
		return desc;
	}

	@Override
	public ITextComponent getHint() {
		return hint;
	}
	
}
