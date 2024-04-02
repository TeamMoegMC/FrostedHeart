package com.teammoeg.frostedheart.content.robotics.logistics;

public class LogisticInternalPushTask implements LogisticTask {
	LogisticSlot from;


	public LogisticInternalPushTask(LogisticSlot from) {
		super();
		this.from = from;
	}


	@Override
	public void work(LogisticNetwork network, int msize) {
		network.importTransit(from, msize);

	}

}
