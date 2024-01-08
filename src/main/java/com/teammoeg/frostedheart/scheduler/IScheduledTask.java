package com.teammoeg.frostedheart.scheduler;

public interface IScheduledTask {
	void executeTask();
	boolean isStillValid();
}
