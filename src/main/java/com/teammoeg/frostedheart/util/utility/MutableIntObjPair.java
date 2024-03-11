package com.teammoeg.frostedheart.util.utility;

public class MutableIntObjPair<V> {
	int first;
	V second;
	public MutableIntObjPair() {
	}
	public MutableIntObjPair(int first,V second) {
		set(first,second);
	}
	public void set(int first,V second) {
		this.first=first;
		this.second=second;
	}
	public int getFirst() {
		return first;
	}
	public void setFirst(int first) {
		this.first = first;
	}
	public V getSecond() {
		return second;
	}
	public void setSecond(V second) {
		this.second = second;
	}
}
