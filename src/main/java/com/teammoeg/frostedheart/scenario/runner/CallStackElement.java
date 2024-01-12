package com.teammoeg.frostedheart.scenario.runner;

public class CallStackElement {
	private int caller;
	private int target;
	public CallStackElement(int caller, int target) {
		super();
		this.caller = caller;
		this.target = target;
	}
	public int getCaller() {
		return caller;
	}
	public int getTarget() {
		return target;
	}
	
}
