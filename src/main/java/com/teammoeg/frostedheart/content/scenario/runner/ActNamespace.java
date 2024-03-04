package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.Objects;

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
		return Objects.hash(act, chapter);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ActNamespace other = (ActNamespace) obj;
		return Objects.equals(act, other.act) && Objects.equals(chapter, other.chapter);
	}
	public boolean isAct() {
		return act!=null&&chapter!=null;
	}
}
