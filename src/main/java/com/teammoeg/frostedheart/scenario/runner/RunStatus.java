package com.teammoeg.frostedheart.scenario.runner;

public enum RunStatus {
	RUNNING(false,true),
	WAITCLIENT(false,true),//waiting for client response
	WAITACTION(false,false),//waiting for player action
	WAITTIMER,//waiting for timer
	WAITTRIGGER(true,false,true),//waiting for async trigger
	STOPPED(true,false,true);//stopped
	public final boolean doPersist;
	public final boolean shouldRun;
	public final boolean shouldPause;

	private RunStatus(boolean doPersist, boolean shouldRun, boolean shouldPause) {
		this.doPersist = doPersist;
		this.shouldRun = shouldRun;
		this.shouldPause = shouldPause;
	}
	private RunStatus(boolean doPersist, boolean shouldRun) {
		this.doPersist = doPersist;
		this.shouldRun = shouldRun;
		this.shouldPause = false;
	}

	private RunStatus() {
		this.doPersist = false;
		this.shouldRun = false;
		this.shouldPause=false;
	}
}
