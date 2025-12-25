package com.teammoeg.frostedresearch.data;

public class UnlockListType<T> {
	public final String name;
	public final Class<T> type;
	public UnlockListType(String name,Class<T> unlockType) {
		super();
		this.name = name;
		this.type=unlockType;
	}


}
