package com.teammoeg.frostedheart.research;

public abstract class FHRegisteredItem {
	private int id=0;
	public int getRId() {
		return id;
	}
	void setRId(int id) {
		this.id=id;
	}
	public abstract String getLId();
}
