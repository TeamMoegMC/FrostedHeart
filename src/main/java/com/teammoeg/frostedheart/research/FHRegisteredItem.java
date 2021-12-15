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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		FHRegisteredItem other = (FHRegisteredItem) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
