package com.teammoeg.frostedheart.research;

import java.util.UUID;

import net.minecraft.util.text.ITextComponent;

public abstract class AbstractClue extends FHRegisteredItem{
	float contribution;
	String ID;
	ITextComponent name;
	ITextComponent desc;
	ITextComponent hint;
	boolean pend;
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
	public boolean isCompleted(UUID team) {
		return ResearchDataManager.INSTANCE.getData(team).isClueTriggered(this);
	}
	public boolean isPendingAtStart() {
		return pend;
	}

	public String getID() {
		return ID;
	}

	public ITextComponent getName() {
		return name;
	}

	public ITextComponent getDescription() {
		return desc;
	}

	public ITextComponent getHint() {
		return hint;
	}
	@Override
	public String getLId() {
		return ID;
	}
}
