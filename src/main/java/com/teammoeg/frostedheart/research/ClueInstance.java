package com.teammoeg.frostedheart.research;

public class ClueInstance {
	protected IClue base;
	protected boolean isStarted;
	protected boolean isCompleted;
	public void onClueEvent(ClueMessage cm) {
		if(!isCompleted) {
			isCompleted=base.onClueEvent(cm);
		}
	};
}
