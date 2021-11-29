package com.teammoeg.frostedheart.research;

import javax.annotation.Nonnull;

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


	

}
