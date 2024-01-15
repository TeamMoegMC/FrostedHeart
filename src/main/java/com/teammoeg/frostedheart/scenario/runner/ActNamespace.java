package com.teammoeg.frostedheart.scenario.runner;

public class ActNamespace {
    public final String chapter;
    public final String act;
    public ActNamespace(String chapter, String quest) {
		super();
		this.chapter = chapter;
		this.act = quest;
	}
    public ActNamespace() {
		super();
		this.chapter = "";
		this.act = "";
	}
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chapter == null) ? 0 : chapter.hashCode());
		result = prime * result + ((act == null) ? 0 : act.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ActNamespace other = (ActNamespace) obj;
		if (!chapter.equals(other.chapter))
			return false;
		if (!act.equals(other.act))
			return false;
		return true;
	}
	public boolean isAct() {
		return !act.isEmpty()&&!chapter.isEmpty();
	}
}
