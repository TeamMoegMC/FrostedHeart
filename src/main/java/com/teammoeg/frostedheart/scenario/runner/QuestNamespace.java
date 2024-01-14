package com.teammoeg.frostedheart.scenario.runner;

public class QuestNamespace {
    public String chapter="";
    public String quest="";
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chapter == null) ? 0 : chapter.hashCode());
		result = prime * result + ((quest == null) ? 0 : quest.hashCode());
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
		QuestNamespace other = (QuestNamespace) obj;
		if (chapter == null) {
			if (other.chapter != null)
				return false;
		} else if (!chapter.equals(other.chapter))
			return false;
		if (quest == null) {
			if (other.quest != null)
				return false;
		} else if (!quest.equals(other.quest))
			return false;
		return true;
	}
	public ImmutableQuestNamespace asImmutable() {
		return new ImmutableQuestNamespace(chapter,quest);
	}
	public void clear() {
		chapter="";
		quest="";
	}
	public boolean has() {
		return !quest.isEmpty()&&!chapter.isEmpty();
	}
}
