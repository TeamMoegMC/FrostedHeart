package com.teammoeg.frostedheart.research;

import javax.annotation.Nonnull;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.util.text.ITextComponent;

public class ClueCopy extends AbstractClue {
	AbstractClue parent;
	public ClueCopy(@Nonnull AbstractClue parent,@Nonnull String ID, float contribution, ITextComponent name, ITextComponent desc, ITextComponent hint,boolean pend) {
		super(ID, contribution, name, desc, hint, pend);
		this.parent=parent;
	}

	@Override
	public float getResearchContribution() {
		return super.contribution==0?parent.getResearchContribution():super.contribution;
	}

	@Override
	public ITextComponent getName() {
		return super.name==null?parent.getName():super.name;
	}

	@Override
	public ITextComponent getDescription() {
		return super.desc==null?parent.getDescription():super.desc;
	}

	@Override
	public ITextComponent getHint() {
		return super.hint==null?parent.getHint():super.hint;
	}

	public int getRId() {
		return parent.getRId();
	}

	public int hashCode() {
		return parent.hashCode();
	}

	public boolean equals(Object obj) {
		return parent.equals(obj);
	}

	public void setCompleted(Team team, boolean trig) {
		parent.setCompleted(team, trig);
	}

	public void setCompleted(boolean trig) {
		parent.setCompleted(trig);
	}

	public boolean isCompleted(TeamResearchData data) {
		return parent.isCompleted(data);
	}

	public boolean isCompleted(Team team) {
		return parent.isCompleted(team);
	}

	public boolean isCompleted() {
		return parent.isCompleted();
	}

	public void sendProgressPacket(Team team) {
		parent.sendProgressPacket(team);
	}

	public boolean isPendingAtStart() {
		return parent.isPendingAtStart();
	}

	public String getID() {
		return parent.getID();
	}

	public String getLId() {
		return parent.getLId();
	}

	public String toString() {
		return parent.toString();
	}


	

}
